package robombs.game.view;

import robombs.game.model.*;

import com.threed.jpct.*;


/**
 * A listener that gets called if a player's bullet has hit another, remote player's model.
 */
public class ClientObjectCollisionListener implements CollisionListener {

	static final private long serialVersionUID=1L;
	
    private transient LocalObject obj = null;
    private transient DecalManager decMan=null;
    
    @SuppressWarnings("unused")
    private boolean local=false;
    
    /**
     * Create a new Listener
     * @param lbm the local object manager that manages all objects of this type that "belong" to this client.
     * @param obj the local object that may be hit
     */
    
    public ClientObjectCollisionListener(LocalObject obj, boolean local) {
        this.obj = obj;
        this.local=local;
    }
    
    public ClientObjectCollisionListener(LocalObject obj, DecalManager decMan, boolean local) {
        this.obj = obj;
        this.local=local;
        this.decMan=decMan;
    }

    public LocalObject getLocalObject() {
    	return obj;
    }
    
    public boolean requiresPolygonIDs() {
        return decMan!=null;
    }

    public void collision(CollisionEvent ce) {
    	if (ce.getSource()!=null) {
    		if (ce.getType()==CollisionEvent.TYPE_TARGET) {
	            Object3D target = ce.getObject();
	            ((CollisionParticipant)ce.getSource()).hits((CollisionParticipant) target, obj, decMan, ce);
    		}
    	}
    }
}
