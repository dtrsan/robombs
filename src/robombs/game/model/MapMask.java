package robombs.game.model;

public class MapMask {

	public final static int UNKNOWN = 0;
	public final static int FLOOR = 1;
	public final static int DEBUG = 255; // For debug only!
	
	public final static int BOMB=99;
	public final static int CRATE=50;
	public final static int BOMB_ITEM=60;
	public final static int FIREPOWER_ITEM=70;
	public final static int KICK_ITEM=80;
	public final static int DISEASE_ITEM=90;
	public final static int NO_BOMB=0;

	public final static int TILE_SIZE = 12;

	public final static int CORNER_NONE = 0;
	public final static int CORNER_DOUBLE = 2;
	public final static int CORNER_SINGLE = 1;
	
	// Note: Player respwan points will marked as 10049 to 10057 but they will be removed
	// right after level loading. The rest of the code should never see them.

	private int[] mask;
	private int width = 0;
	private int height = 0;

	public MapMask(int width, int height) {
		this.width = width;
		this.height = height;
		mask = new int[width * height];
	}
	
	public void clear() {
		for (int i=0; i<mask.length; i++) {
			mask[i]=UNKNOWN;
		}
	}
	
	public static boolean isItem(int val) {
		return val==BOMB_ITEM || val==FIREPOWER_ITEM || val==KICK_ITEM || val==DISEASE_ITEM;
	}
	
	public MapMask cloneMask() {
		MapMask clone=new MapMask(getWidth(), getHeight());
		int cnt=0;
		for (int i:mask) {
			clone.mask[cnt]=i;
			cnt++;
		}
		return clone;
	}

	public GridPosition getGrid(float x, float z) {
		int px = (int) (x / (float) MapMask.TILE_SIZE);
		int pz = (int) (z / (float) MapMask.TILE_SIZE + 1);
		return new GridPosition(px, pz);
	}
	
	// Ecke bzw. über die Kante?
	public int doCornerCheck(GridPosition cur, int xOff, int zOff) {
		if (xOff != 0 && zOff != 0) {
			int x = cur.getX();
			int z = cur.getZ();
			boolean cx = isObstacle(x + xOff, z);
			boolean cz = isObstacle(x, z + zOff);
			if (cx && cz) {
				return CORNER_DOUBLE;
			} else {
				if (cx || cz) {
					return CORNER_SINGLE;
				}
			}
		}
		return CORNER_NONE;
	}

	// Festes Hinternis?
	public boolean isObstacle(int x, int y) {
		int tile = getMaskAt(x, y);
		return tile == MapMask.UNKNOWN;
	}

	// Temporäres Hindernis?
	public boolean isBlocked(int x, int y) {
		int tile = getMaskAt(x, y);
		return tile==MapMask.CRATE || isItem(tile);
	}
	
	public boolean isSemiBlocked(int x, int y) {
		int tile = getMaskAt(x, y);
		return tile==MapMask.CRATE;
	}
	
	// Checks if a position is in a bomb's range.
	public boolean isInBombRange(int x, int y, int minDist) {
		for (int i=-minDist; i<=minDist; i++) {
			//if (i!=0) {
				if (getMaskAt(x+i,y)==MapMask.BOMB) {
					return true;
				}
				if (getMaskAt(x,y+i)==MapMask.BOMB) {
					return true;
				}
			//}
		}
		return false;
	}

	public int[] getMask() {
		return mask;
	}

	public synchronized void setMaskAt(int x, int y, int value) {
		if (y>=0 && y<height && x>=0 && x<width) {
			mask[y * width + x] = value;
		}
	}

	public void setMaskAt(GridPosition gp, int value) {
		setMaskAt(gp.getX(), gp.getZ(), value);
	}

	public int getMaskAt(GridPosition gp) {
		return getMaskAt(gp.getX(), gp.getZ());
	}

	public synchronized int getMaskAt(int x, int y) {
		if (x < 0 || y < 0 || y>=height || x>=width) {
			return UNKNOWN;
		}
		return mask[y * width + x];
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public boolean isNearBy(int item, GridPosition gp) {
		for (int x=-1; x<2; x++) {
			for (int y=-1; y<2; y++) {
				if (this.getMaskAt(gp.getX()+x, gp.getZ()+y)==item) {
					return true;
				}
			}
		}
		return false;
	}
	
	public String toString() {
		String r = "";
		for (int y = 0; y < height; y++) {
			StringBuffer sb = new StringBuffer(width);
			for (int x = 0; x < width; x++) {
				int pos = x + y * width;
				int m = mask[pos];
				if (m != UNKNOWN) {
					if (m != DEBUG) {
						if (m==BOMB) {
							sb.append('B');
						} else {
							sb.append(Character.toString((char)(m+47)));
						}
					} else {
						sb.append('.');
					}
				} else {
					sb.append('*');
				}
			}
			r += sb.append("\n").toString();
		}
		return r;
	}

}