package robombs.game.model;

import com.threed.jpct.CollisionEvent;

import robombs.game.view.*;

/**
 * A (more or less) "dummy" class used to make the player an actual collision source in the listeners.
 */
public class PlayerDummy extends ClientObject {
	static final private long serialVersionUID=1L;
	
	private ClientObject org=null;
	
	public PlayerDummy(int id) {
		this.setClientID(id);
	}
	
	public void setMaster(ClientObject obj) {
		org=obj;
	}
	
	public LocalObject getModel() {
		return org.getModel();
	}
	
	 public void hits(CollisionParticipant target, LocalObject obj, DecalManager decal, CollisionEvent ce) {
		 target.hitByLocalPlayer(this, obj, decal, ce);
	 }
}
