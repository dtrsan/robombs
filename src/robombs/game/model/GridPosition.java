package robombs.game.model;

import com.threed.jpct.*;

public class GridPosition {

	protected int x;
	protected int z;

	public GridPosition() {
		this(-9999, -9999);
	}

	public GridPosition(int x, int z) {
		this.x = x;
		this.z = z;
	}

	public float getDistanceTo(GridPosition gp) {
		int dx = gp.x - x;
		int dz = gp.z - z;
		return (float) Math.sqrt(dx * dx + dz * dz);
	}

	public void set(int x, int z) {
		this.x = x;
		this.z = z;
	}

	public SimpleVector convertTo3D() {
		float xp = x * MapMask.TILE_SIZE + (MapMask.TILE_SIZE >> 1);
		float zp = z * MapMask.TILE_SIZE - (MapMask.TILE_SIZE >> 1);
		return new SimpleVector(xp, 0, zp);
	}

	public int getX() {
		return x;
	}

	public int getZ() {
		return z;
	}

	public String toString() {
		String h = Integer.toString(x);
		String w = Integer.toString(z);
		if (x < 10 && x >= 0) {
			h = '0' + h;
		}
		if (z < 10 && z >= 0) {
			w = '0' + w;
		}

		return h + "/" + w;
	}

	public boolean equals(Object pos) {
		if (pos instanceof GridPosition) {
			GridPosition gp = (GridPosition) pos;
			return gp.x == x && gp.z == z;
		}
		return false;
	}

	public int hashCode() {
		return x * 10000 + z;
	}

}
