package robombs.game.view;

import java.util.*;

import robombs.game.*;
import robombs.game.util.*;
import robombs.game.model.*;
import robombs.clientserver.*;

import com.threed.jpct.*;

public class LevelLoader {

	private final static char WALL = '*';
	private final static char NOTHING = ' ';
	private final static char OUTSIDE = '.';
	private final static char CRATE = 'c';
	private final static char BOMB_ITEM = 'b';
	private final static char KICK_ITEM = 'k';
	private final static char DISEASE_ITEM = 'd';
	private final static char FIREPOWER_ITEM = 'f';

	private final static String WALL_TEXTURE = "wall";
	private final static String WALL_TEXTURE_DARK = "wall_dark";
	private final static String FLOOR_TEXTURE = "floor";
	private final static String TOP_TEXTURE = "top";
	private final static String BORDER_TEXTURE = "border";
	private final static String BLACK_TEXTURE = "black";

	private static int tessellation = 1;
	private static SimpleVector s1 = new SimpleVector();
	private static SimpleVector s2 = new SimpleVector();
	private static SimpleVector s3 = new SimpleVector();

	private static final Map<String, List<Object>> cache=new HashMap<String, List<Object>>();

	private static void addVerticalPolygon(Object3D obj, float xMin,
			float xMax, float yMin, float yMax, float zMin, float zMax,
			int texID) {
		// texID++;

		int tessellation=LevelLoader.tessellation;
		if (texID==0) {
			tessellation=1;
		}

		float ft = (float) tessellation;


		float dX = (xMax - xMin) / ft;
		float dY = (yMax - yMin) / ft;
		float dZ = (zMax - zMin) / ft;
		float du = 1f / ft;
		float dv = du;

		int xTes = tessellation;
		int yTes = tessellation;
		int zTes = tessellation;

		if (dX == 0) {
			xTes = 1;
		}

		if (dZ == 0) {
			zTes = 1;
		}

		for (int xi = 0; xi < xTes; xi++) {
			float xs = xMin + dX * (float) xi;
			float xe = xs + dX;

			for (int yi = 0; yi < yTes; yi++) {

				float ys = yMin + dY * (float) yi;
				float ye = ys + dY;

				float vs = dv * (float) (yi);
				float ve = vs + dv;

				for (int zi = 0; zi < zTes; zi++) {
					float zs = zMin + dZ * (float) zi;
					float ze = zs + dZ;

					float zt = zi;
					if (zTes == 1) {
						zt = xi;
					}
					float us = du * zt;
					float ue = us + du;

					s1.set(xs, ye, zs);
					s2.set(xe, ye, ze);
					s3.set(xe, ys, ze);
					obj.addTriangle(s1, us, ve, s2, ue, ve, s3, ue, vs, texID);

					s2.set(xs, ys, zs);
					obj.addTriangle(s3, ue, vs, s2, us, vs, s1, us, ve, texID);
				}
			}
		}
	}

	private static void addHorizontalPolygon(Object3D obj, float xMin,
			float xMax, float y, float zMin, float zMax, int tesselation,
			int texID) {

		float ft = (float) tesselation;
		float dX = (xMax - xMin) / ft;
		float dZ = (zMax - zMin) / ft;
		float du = 1f / ft;
		float dv = du;

		for (int xi = 0; xi < tesselation; xi++) {
			float xs = xMin + dX * (float) xi;
			float xe = xs + dX;
			float us = du * xi;
			float ue = us + du;

			for (int zi = 0; zi < tesselation; zi++) {
				float zs = zMin + dZ * (float) zi;
				float ze = zs + dZ;
				float vs = dv * zi;
				float ve = vs + dv;

				s1.set(xs, y, ze);
				s2.set(xe, y, ze);
				s3.set(xe, y, zs);
				obj.addTriangle(s1, us, ve, s2, ue, ve, s3, ue, vs, texID);
				s2.set(xs, y, zs);
				obj.addTriangle(s3, ue, vs, s2, us, vs, s1, us, ve, texID);
			}
		}
	}

	private static int countElements(String level, char chr) {
		int cnt = 0;
		for (int i = 0; i < level.length(); i++) {
			if (level.charAt(i) == chr) {
				cnt++;
			} else {
				if (chr == '?') {
					char c = level.charAt(i);
					if (c == NOTHING || c==CRATE || c==BOMB_ITEM || c==FIREPOWER_ITEM || c==KICK_ITEM || c==DISEASE_ITEM) {
						// Alles andere....
						cnt++;
					}
				}
			}

		}
		return cnt;
	}

	static synchronized List<Object> loadMap(String file, String set) throws Exception {

		set+="_";
		long t=Ticker.getTime();

		List<Object> fc=getFromCache(file);
		if (fc!=null) {
			NetLogger.log("Got level data for '"+file+"' from cache in "+(Ticker.getTime()-t)+"ms.");
			return fc;
		}

		SimpleVector minBB = new SimpleVector(99999999, 0, 99999999);
		SimpleVector maxBB = new SimpleVector(-99999999, 0, -99999999);

		SimpleStream stream = new SimpleStream(file);
		String level = Loader.loadTextFile(stream.getStream());
		stream.close();
		int wallCnt = countElements(level, WALL);
		int tesQ = tessellation * tessellation;
		// Die Zahlen bei den Walls hier sind irgendwie geraten...:-)
		int size=wallCnt * tesQ * 4 + 1000;
		Object3D walls = new Object3D(size);

		// Shadows is a special, simplified mesh inside the walls that is used for
		// shadow casting. Otherwise the walls would have to be caster and receiver,
		// which will cause shadow acne on most systems.
		Object3D shadows = new Object3D(size);
		float soffs=Globals.shadowMeshOffset;

		Object3D floor = new Object3D(countElements(level, '?') * tesQ * 2+100);
		int topCnt = 0;

		TextureManager texMan = TextureManager.getInstance();

		StringTokenizer toky = new StringTokenizer(level, "\n");
		String[] lines = new String[toky.countTokens() + 2];
		int cnt = 0;
		int max = -1;

		StringBuffer buf = new StringBuffer(40);
		StringBuffer buf2 = new StringBuffer(40);
		while (toky.hasMoreTokens()) {
			String tok = toky.nextToken();
			buf.setLength(0);
			buf2.setLength(0);
			int start = 0;
			int end = 0;

			if (max < tok.length() + 1) {
				max = tok.length() + 1;
			}

			buf.append(OUTSIDE);
			topCnt++;
			final int endy = tok.length();
			for (int i = 0; i < endy; i++) {
				char c = tok.charAt(i);
				if (c == WALL) {
					start = i;
					break;
				} else {
					buf.append(OUTSIDE);
					topCnt++;
				}
			}

			for (int i = endy - 1; i >= 0; i--) {
				char c = tok.charAt(i);
				if (c == WALL) {
					end = i;
					break;
				} else {
					buf2.append(OUTSIDE);
					topCnt++;
				}
			}

			buf.append(tok.substring(start, end + 1)).append(buf2);

			lines[cnt + 1] = buf.toString();
			cnt++;
		}

		StringBuffer sb = new StringBuffer(max);
		for (int i = 0; i < max; i++) {
			sb.append(OUTSIDE);
		}

		String sbs = sb.toString();
		lines[0] = sbs;
		lines[cnt + 1] = sbs;

		int endy = lines.length;
		for (int i = 0; i < endy; i++) {
			String l = lines[i];
			if (l.length() < max) {
				lines[i] += sbs.substring(0, max - l.length());
				topCnt += max - l.length();
			}
			lines[i]=flip(lines[i]);
			System.out.println(lines[i]);
		}

		level = null;
		String tmpLine = null;

		Object3D top = new Object3D(topCnt * 2 + wallCnt * 2);

		MapMask mask = new MapMask(max, lines.length);

		int texID_dark;
		texID_dark = texMan.getTextureID(set+WALL_TEXTURE_DARK);

		for (int i = 1; i < cnt + 1; i++) {
			String line = lines[i];
			int z = i * MapMask.TILE_SIZE;
			boolean foundWall = false;
			endy = line.length();
			for (int p = 0; p < endy; p++) {
				char c = line.charAt(p);
				if (foundWall || c == WALL) {
					int x = p * MapMask.TILE_SIZE;

					String topy=TOP_TEXTURE;
					int height=7;
					if (i==1 || i==cnt || p==1 || p==endy-2) {
						height=8; // 10
						topy=BORDER_TEXTURE;
					}

					switch (c) {
					case (WALL): {

						int cto=NOTHING;
						int ctu=NOTHING;
						int ctl=NOTHING;
						int ctr=NOTHING;
						// oben:
						if (i > 0) {
							tmpLine = lines[i - 1];
							if (tmpLine.length() > p) {
								cto = tmpLine.charAt(p);
							}
						}

						if (i < cnt + 1) {
							tmpLine = lines[i + 1];
							if (tmpLine.length() > p) {
								ctu = tmpLine.charAt(p);
							}
						}

						if (p > 0) {
							ctl = line.charAt(p - 1);
						}

						if (p < line.length() - 1) {
							ctr = line.charAt(p + 1);
						}

						int texID = texMan.getTextureID(set+topy);

						// "Dach" malen...da reicht einfache
						// Geometrie-Auflösung, da es
						// nicht beleuchtet wird
						addHorizontalPolygon(top, x, x + MapMask.TILE_SIZE,
								-height, z, z - MapMask.TILE_SIZE, 1, texID);
						
						addHorizontalPolygon(shadows, x + MapMask.TILE_SIZE -(ctr!=WALL?soffs/10f:0), x+(ctl!=WALL?soffs/10f:0),
								-height, z-(ctu!=WALL?soffs/10f:0), z- MapMask.TILE_SIZE+(cto!=WALL?soffs/10f:0), 1, texID);

						texID = texMan.getTextureID(set+WALL_TEXTURE);

						extendBB(minBB, maxBB, x, x + MapMask.TILE_SIZE, z, z
								- MapMask.TILE_SIZE);

						// mask.setMaskAt(p,i,MapMask.UNKNOWN);

						int sideCnt = 0;


						if (cto != WALL || i==lines.length-2) {
							addVerticalPolygon(walls, x, x + MapMask.TILE_SIZE,
									-height, 0, z - MapMask.TILE_SIZE, z
											- MapMask.TILE_SIZE, texID);


								addVerticalPolygon(shadows, x+(ctl!=WALL?soffs/10f:0), x + MapMask.TILE_SIZE-(ctr!=WALL?soffs/10f:0),
									-height, 1, z - MapMask.TILE_SIZE+soffs/10f, z
											- MapMask.TILE_SIZE+soffs/10f, 0);

							sideCnt++;
						}

						// unten:
						if (ctu != WALL || i==1) {
							addVerticalPolygon(walls, x + MapMask.TILE_SIZE, x,
									-height, 0, z, z, texID_dark);

								addVerticalPolygon(shadows, x + MapMask.TILE_SIZE-(ctr!=WALL?soffs/10f:0), x+(ctl!=WALL?soffs/10f:0),
									-height, 1, z-soffs/10f, z-soffs/10f, 0);

							sideCnt++;
						}

						// links:
						if (ctl != WALL || p==line.length()-2) {
							addVerticalPolygon(walls, x, x, -height, 0, z, z
									- MapMask.TILE_SIZE, texID_dark);

							addVerticalPolygon(shadows, x+soffs/10f, x+soffs/10f, -height, 1, z-(ctu!=WALL?soffs/10f:0), z
									- MapMask.TILE_SIZE+(cto!=WALL?soffs/10f:0), 0);

							sideCnt++;
						}

						// rechts
						if (ctr != WALL || p==1) {
							addVerticalPolygon(walls, x + MapMask.TILE_SIZE, x
									+ MapMask.TILE_SIZE, -height, 0, z
									- MapMask.TILE_SIZE, z, texID);

							addVerticalPolygon(shadows, x + MapMask.TILE_SIZE-soffs/10f, x
									+ MapMask.TILE_SIZE-soffs/10f, -height, 1, z
									- MapMask.TILE_SIZE+(cto!=WALL?soffs/10f:0), z-(ctu!=WALL?soffs/10f:0), 0);

							sideCnt++;
						}

						break;
					}

					default: {
						// "Boden" malen

						if (c != OUTSIDE) {
							int texID = 0;
							if (c == NOTHING || c==CRATE || c==BOMB_ITEM || c==KICK_ITEM || c==FIREPOWER_ITEM || c==DISEASE_ITEM || (c>='1' && c<='9')) {
								String floorTex = set+FLOOR_TEXTURE;
								int f = (int) (Math.random()*100);
								if (f<12) {
									floorTex += 4;
								} else {
									if (f<24) {
										floorTex += 3;
									} else {
										if (f<36) {
											floorTex += 2;
										}
									}
								}
								texID = texMan.getTextureID(floorTex);
								if (c==CRATE) {
									mask.setMaskAt(p, i, MapMask.CRATE);
								}
								if (c==BOMB_ITEM) {
									mask.setMaskAt(p, i, MapMask.BOMB_ITEM);
								}
								if (c==FIREPOWER_ITEM) {
									mask.setMaskAt(p, i, MapMask.FIREPOWER_ITEM);
								}
								if (c==KICK_ITEM) {
									mask.setMaskAt(p, i, MapMask.KICK_ITEM);
								}
								if (c==DISEASE_ITEM) {
									mask.setMaskAt(p, i, MapMask.DISEASE_ITEM);
								}
								if (c==NOTHING) {
									mask.setMaskAt(p, i, MapMask.FLOOR);
								}
								if (c>='1' && c<='9') {
									mask.setMaskAt(p,i,((int)c)+10000);
								}
							}

							addHorizontalPolygon(floor, x, x
									+ MapMask.TILE_SIZE, 0, z, z
									- MapMask.TILE_SIZE, tessellation, texID);
						} else {
							if (p > 0&& p < endy - 1) {
								// Die Kanten brauchen das nicht...
								int texID = texMan.getTextureID(set+BLACK_TEXTURE);
								addHorizontalPolygon(top, x, x
										+ MapMask.TILE_SIZE, -10, z, z
										- MapMask.TILE_SIZE, 1, texID);
							}
						}
					}
					}
				}
				foundWall = true;
			}
		}

		List<Object> v = new ArrayList<Object>(6);
		v.add(new LevelPart(walls));
		v.add(new LevelPart(top));
		v.add(new LevelPart(floor));
		v.add(mask);
		v.add(minBB);
		v.add(maxBB);
		shadows.setTransparency(-1);
		if (!Globals.shadowCulling) {
			shadows.invert();
		}
		v.add(new LevelPart(shadows));

		storeInCache(file, v);

		NetLogger.log("Loaded level data for '"+file+"' in "+(Ticker.getTime()-t)+"ms");

		return v;
	}

	private static String flip(String line) {
		StringBuffer sb=new StringBuffer();
		for (int i=line.length()-1; i>=0; i--) {
			sb.append(line.charAt(i));
		}
		return sb.toString();
	}
	
	private static void storeInCache(String name, List<Object> v) {
		List<Object> c=new ArrayList<Object>(v);
		copy(c);
		cache.clear(); // The cache holds one entry only ATM
		cache.put(name, c);
	}

	private static List<Object> getFromCache(String name) {
		List<Object> c=cache.get(name);
		if (c!=null) {
			c=new ArrayList<Object>(c);
			copy(c);
		}
		return c;
	}

	private static void copy(List<Object> c) {
		for (int i=0;i<3; i++) {
			LevelPart lp=new LevelPart((LevelPart) c.get(i));
			lp.setMesh(lp.getMesh().cloneMesh(true));
			c.set(i, lp);
		}
		c.set(3, ((MapMask) c.get(3)).cloneMask());
		c.set(4, new SimpleVector((SimpleVector) c.get(4)));
		c.set(5, new SimpleVector((SimpleVector) c.get(5)));
	}

	private static void extendBB(SimpleVector min, SimpleVector max, float x1,
			float x2, float z1, float z2) {
		if (min.x > x1) {
			min.x = x1;
		}
		if (min.x > x2) {
			min.x = x2;
		}
		if (min.z > z1) {
			min.z = z1;
		}
		if (min.z > z2) {
			min.z = z2;
		}
		if (max.x < x1) {
			max.x = x1;
		}
		if (max.x < x2) {
			max.x = x2;
		}
		if (max.z < z1) {
			max.z = z1;
		}
		if (max.z < z2) {
			max.z = z2;
		}
	}
}
