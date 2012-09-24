package robombs.game.view;

import robombs.game.*;
import robombs.game.model.*;

import com.threed.jpct.*;
import java.util.*;

public class CollisionMesh extends Object3D implements CollisionParticipant  {

	static final private long serialVersionUID=1L;
	private static Map<Float, Object3D> bluePrints= new HashMap<Float, Object3D>();
	
	private ClientObject org=null;
	
	public CollisionMesh(ClientObject orig, float size) {
		// The caching of these objects is not optimal...too much memory is needed in the process...
		super(bluePrints.get(size)==null?Primitives.getCylinder(12, size, 1.3f):bluePrints.get(size), true);
		if (bluePrints.get(size)==null) {
			bluePrints.put(size, Primitives.getCylinder(12, size, 1.3f));
		} 
		// Do the build() "by hand"...this way, we can skip one unneeded calculation.
		calcCenter();
		calcBoundingBox();
		calcNormals();
		translate(0, size, 0);
		this.org=orig;
	}
	
	public void hits(CollisionParticipant target, LocalObject obj, DecalManager decal, CollisionEvent ce) {
		org.hits(target, obj, decal, ce);
	}
	
	public void hitByExplosion(CollisionParticipant source, LocalObject obj, DecalManager decal, CollisionEvent ce) {
		org.hitByExplosion(source, obj, decal, ce);
	}
	
	public void hitByLocalPlayer(CollisionParticipant source, LocalObject obj, DecalManager decal, CollisionEvent ce) {
		org.hitByLocalPlayer(source, obj, decal, ce);
	}

	public void setClientID(int id) {
		org.setClientID(id);
	}
	
	public void setObjectID(int id) {
		org.setObjectID(id);
	}
	
	public int getObjectID() {
		return org.getObjectID();
	}
	
	public LocalObject getModel() {
		return org.getModel();
	}
	
	public int getClientID() {
		return org.getClientID();
	}
	
	public void setEventQueue(ClientEventQueue q) {
		org.setEventQueue(q);
	}
	
	public ClientEventQueue getEventQueue() {
		return org.getEventQueue();
	}
}
