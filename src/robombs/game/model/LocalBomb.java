package robombs.game.model;

import robombs.game.Globals;

import com.threed.jpct.*;

public class LocalBomb extends LocalObject {

	private boolean moves=false;
	
	public LocalBomb(int clientID) {
        super(clientID);
    }
	
	public void setDirection(SimpleVector dir) {
		dir.scalarMul(Globals.bulletSpeed*2f);
		setSpeed(dir);
		moves=true;
	}
	
	public void stop() {
		moves=false;
	}
	
	public boolean isMoving() {
		return moves;
	}
	
	public LocalBomb() {
		throw new RuntimeException("A bomb has to belong to a client!");
	}
}
