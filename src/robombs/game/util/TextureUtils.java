package robombs.game.util;

import robombs.game.Globals;

import com.threed.jpct.*;

public class TextureUtils {

	private static Texture alienBluePrint = null;
	private static int[] pixelsBluePrint = null;

	static {
		try {
			SimpleStream ss = new SimpleStream("data/disco.jpg");
			alienBluePrint = new Texture(ss.getStream());
			ss.close();

			ITextureEffect it = new Opaquerizer();
			alienBluePrint.setEffect(it);
			alienBluePrint.applyEffect();
			alienBluePrint.removeEffect();

			PixelGrabber grabber = new PixelGrabber();
			alienBluePrint.setEffect(grabber);
			alienBluePrint.applyEffect();
			alienBluePrint.removeEffect();

			pixelsBluePrint = grabber.getPixels();
		} catch (Exception e) {
			throw new RuntimeException("Unable to load alien skin!", e);
		}
	}

	public static Texture initOpaqueTexture(String name, String file) throws Exception {
		Texture t = initTexture(name, file, true);
		ITextureEffect it = new Opaquerizer();
		t.setEffect(it);
		t.applyEffect();
		t.removeEffect();
		return t;
	}

	public static Texture initTexture(String name, String file, boolean opaque) throws Exception {
		SimpleStream ss = new SimpleStream(file);
		Texture t = null;
		t = new Texture(ss.getStream());
		TextureManager.getInstance().addTexture(name, t);
		if (opaque) {
			t.removeAlpha();
		}
		ss.close();
		return t;
	}

	public static Texture initAlphaTexture(String name, String file) throws Exception {
		SimpleStream ss = new SimpleStream(file);
		Texture t = null;
		t = new Texture(ss.getStream(), true);
		TextureManager.getInstance().addTexture(name, t);
		ss.close();
		return t;
	}

	public static Texture colorizeAlienSkin(float redFac, float greenFac, float blueFac) {
		Texture newy = new Texture(alienBluePrint.getWidth(), alienBluePrint.getHeight());

		PixelGrabber grabber = new PixelGrabber();
		int[] res = new int[newy.getArraySize()];

		for (int x = 0; x < newy.getWidth(); x++) {
			for (int y = 0; y < newy.getHeight(); y++) {
				int pos = y * newy.getWidth() + x;
				if (x < 3 || x > 62 || y < 62 || y > 86) {
					// Spare the eyes!
					int col = pixelsBluePrint[pos] & 0x00ffffff;
					int r = col >> 16;
					int g = (col >> 8) & 0xff;
					int b = col & 0xff;
					int dif = Math.abs(r - g) + Math.abs(g - b);
					if (dif <= 30) {
						r *= redFac;
						g *= greenFac;
						b *= blueFac;
					}

					if (r > 255) {
						r = 255;
					}
					if (g > 255) {
						g = 255;
					}
					if (b > 255) {
						b = 255;
					}

					res[pos] = (r << 16) | (g << 8) | b;
				} else {
					res[pos] = pixelsBluePrint[pos];
				}
			}
		}
		newy.setEffect(grabber);
		grabber.setNewPixels(res);
		newy.applyEffect();
		newy.removeEffect();

		return newy;
	}

	public static void loadGameTextures() throws Exception {

		long s = Ticker.getTime();
		// Load in two threads to utilize multicore-cpus for a faster startup.
		Thread loader = new Thread() {
			public void run() {
				try {
					initOpaqueTexture("warehouse_top", "data/sets/warehouse/top.jpg");
					initOpaqueTexture("warehouse_border", "data/sets/warehouse/border.jpg");
					initTexture("warehouse_wall", "data/sets/warehouse/wall.jpg", true);
					initTexture("warehouse_wall_dark", "data/sets/warehouse/wall_dark.jpg", true);
					initTexture("warehouse_floor", "data/sets/warehouse/floor.jpg", true);
					initTexture("warehouse_floor2", "data/sets/warehouse/floor2.jpg", true);
					initTexture("warehouse_floor3", "data/sets/warehouse/floor3.jpg", true);
					initTexture("warehouse_floor4", "data/sets/warehouse/floor4.jpg", true);

					initOpaqueTexture("japan_top", "data/sets/japan/top.jpg");
					initOpaqueTexture("japan_border", "data/sets/japan/border.jpg");
					initTexture("japan_wall", "data/sets/japan/wall.jpg", true);
					initTexture("japan_wall_dark", "data/sets/japan/wall_dark.jpg", true);
					initTexture("japan_floor", "data/sets/japan/floor.jpg", true);
					initTexture("japan_floor2", "data/sets/japan/floor2.jpg", true);
					initTexture("japan_floor3", "data/sets/japan/floor3.jpg", true);
					initTexture("japan_floor4", "data/sets/japan/floor4.jpg", true);

					initOpaqueTexture("forest_top", "data/sets/forest/top.jpg");
					initOpaqueTexture("forest_border", "data/sets/forest/border.jpg");
					initTexture("forest_wall", "data/sets/forest/wall.jpg", true);
					initTexture("forest_wall_dark", "data/sets/forest/wall_dark.jpg", true);
					initTexture("forest_floor", "data/sets/forest/floor.jpg", true);
					initTexture("forest_floor2", "data/sets/forest/floor2.jpg", true);
					initTexture("forest_floor3", "data/sets/forest/floor3.jpg", true);
					initTexture("forest_floor4", "data/sets/forest/floor4.jpg", true);

					initOpaqueTexture("medival_top", "data/sets/medival/top.jpg");
					initOpaqueTexture("medival_border", "data/sets/medival/border.jpg");
					initTexture("medival_wall", "data/sets/medival/wall.jpg", true);
					initTexture("medival_wall_dark", "data/sets/medival/wall_dark.jpg", true);
					initTexture("medival_floor", "data/sets/medival/floor.jpg", true);
					initTexture("medival_floor2", "data/sets/medival/floor2.jpg", true);
					initTexture("medival_floor3", "data/sets/medival/floor3.jpg", true);
					initTexture("medival_floor4", "data/sets/medival/floor4.jpg", true);

					initOpaqueTexture("space_top", "data/sets/space/top.jpg");
					initOpaqueTexture("space_border", "data/sets/space/border.jpg");
					initTexture("space_wall", "data/sets/space/wall.jpg", true);
					initTexture("space_wall_dark", "data/sets/space/wall_dark.jpg", true);
					initTexture("space_floor", "data/sets/space/floor.jpg", true);
					initTexture("space_floor2", "data/sets/space/floor2.jpg", true);
					initTexture("space_floor3", "data/sets/space/floor3.jpg", true);
					initTexture("space_floor4", "data/sets/space/floor4.jpg", true);
					
					initTexture("highback", "data/highback.png", false);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		};
		loader.start();

		initOpaqueTexture("classic_top", "data/sets/classic/top.jpg");
		initOpaqueTexture("classic_border", "data/sets/classic/border.jpg");
		initTexture("classic_wall", "data/sets/classic/wall.jpg", true);
		initTexture("classic_wall_dark", "data/sets/classic/wall_dark.jpg", true);
		initTexture("classic_floor", "data/sets/classic/floor.jpg", true);
		initTexture("classic_floor2", "data/sets/classic/floor2.jpg", true);
		initTexture("classic_floor3", "data/sets/classic/floor3.jpg", true);
		initTexture("classic_floor4", "data/sets/classic/floor4.jpg", true);

		initOpaqueTexture("southern_top", "data/sets/southern/top.jpg");
		initOpaqueTexture("southern_border", "data/sets/southern/border.jpg");
		initTexture("southern_wall", "data/sets/southern/wall.jpg", true);
		initTexture("southern_wall_dark", "data/sets/southern/wall_dark.jpg", true);
		initTexture("southern_floor", "data/sets/southern/floor.jpg", true);
		initTexture("southern_floor2", "data/sets/southern/floor2.jpg", true);
		initTexture("southern_floor3", "data/sets/southern/floor3.jpg", true);
		initTexture("southern_floor4", "data/sets/southern/floor4.jpg", true);

		initOpaqueTexture("comic_top", "data/sets/comic/top.jpg");
		initOpaqueTexture("comic_border", "data/sets/comic/border.jpg");
		initTexture("comic_wall", "data/sets/comic/wall.jpg", true);
		initTexture("comic_wall_dark", "data/sets/comic/wall_dark.jpg", true);
		initTexture("comic_floor", "data/sets/comic/floor.jpg", true);
		initTexture("comic_floor2", "data/sets/comic/floor2.jpg", true);
		initTexture("comic_floor3", "data/sets/comic/floor3.jpg", true);
		initTexture("comic_floor4", "data/sets/comic/floor4.jpg", true);

		initTexture("bomb1", "data/bomb.jpg", true);
		//initTexture("boom", "data/boom.png", false);
		initTexture("crate", "data/crate.jpg", true);
		initAlphaTexture("blood", "data/blood.png");
		initTexture("explosion", "data/explosion.png", false);

		initTexture("particle", "data/particle.jpg", false);
		initTexture("star", "data/star.png", false);
		initTexture("slime", "data/slime.png", false);
		initTexture("glow", "data/glow.jpg", false);
		initTexture("spark", "data/spark.gif", false);
		initTexture("crosshair", "data/crosshair.gif", false);
		initTexture("decal", "data/decal.gif", false);

		if (!Globals.compiledObjects) {
			initTexture("item", "data/itemenv.jpg", true);
		} else {
			initTexture("item", "data/itemenv_rev.jpg", true);
		}
		initTexture("icons", "data/icons.png", false);
		TextureManager.getInstance().getTexture("icons").enableGLClamping();

		initTexture("smoke", "data/smoke2.jpg", true);
		initTexture("shield", "data/shield.png", true);
		initTexture("black", "data/justblack.png", true);
		initTexture("bar", "data/bar.png", true);

		initTexture("barbox", "data/box.png", false);

		initTexture("drop2", "data/drop2.png", false);

		initAlphaTexture("drop", "data/drop.png");
		initAlphaTexture("debris", "data/debris.png");
		initAlphaTexture("textbanner", "data/banner.png");
		initAlphaTexture("textbannerLost", "data/banner_dead.png");

		initTexture("credits", "data/credits.png", true);
		initTexture("crazy", "data/crazy.png", true);
		
		// Normalmaps
		initTexture("cratenormals", "data/crate_normals.png", true);
		initTexture("bombnormals", "data/bomb_normals.png", true);

		if (Globals.skyBox) {
			initTexture("left", "data/skybox/left.png", true);
			initTexture("right", "data/skybox/right.png", true);
			initTexture("up", "data/skybox/up.png", true);
			initTexture("down", "data/skybox/down.png", true);
			initTexture("front", "data/skybox/front.png", true);
			initTexture("back", "data/skybox/back.png", true);
		}

		loader.join();
		System.out.println("Textures loaded in " + (Ticker.getTime() - s) + "ms!");
	}

	private static class PixelGrabber implements ITextureEffect {

		private int[] pixels = null;
		private int[] toSet = null;

		public void setNewPixels(int[] pixels) {
			toSet = pixels;
		}

		public void init(Texture t) {
		}

		public void apply(int[] dest, int[] src) {
			if (toSet == null) {
				pixels = new int[src.length];
				System.arraycopy(src, 0, pixels, 0, src.length);
			} else {
				System.arraycopy(toSet, 0, dest, 0, dest.length);
			}
		}

		public boolean containsAlpha() {
			return false;
		}

		public int[] getPixels() {
			return pixels;
		}

	}

}
