package robombs.game.view;

import robombs.game.model.*;
import robombs.game.util.*;
import robombs.clientserver.*;
import robombs.game.*;

import com.threed.jpct.*;

/**
 * A client object is the visual presentation of a LocalObject. Because we are using a 3D presentation
 * here, its simply an extended version of jPCT's Object3D class. In addition to a normal Object3D, a ClientObject
 * holds a few "backbuffer" values, that has been set based on new data from the server but that hasn't been
 * used in the view until now.
 */
public class ClientObject extends Object3D implements CollisionParticipant {

	static final private long serialVersionUID=1L;
	
    private boolean isModified = false;
    private Matrix backRot = new Matrix();
    private SimpleVector backPos = new SimpleVector();
    private SimpleVector speed = new SimpleVector();
    private int backAnim = 0;
    private int backAnimSpeed = 0;
    private long lastAccess = 0;
    private long ticks = 0;
    private int lastSequence = -1;
    private int lastAnimSpeed = 1;
    private boolean isRemote = false;
    private boolean isTransfered=false;
    private boolean localOnly=false;
    private int value=0;
    private int clientID=-99999999;
    private int objectID=-99;
    private boolean childAnimVisible=true;
    
    private transient ClientEventQueue eventQueue=null;
    
    protected ChildObject3D child = null;
    protected CollisionMesh colMesh=null;
    protected boolean collisionMode=false;
    
    protected LocalObject model=null; // This is a cyclic reference...bad, but needed!
    

    /**
     * Creates a new, empty client object. This can be used as a "glue" object between other, real, objects.
     * Just like the dummy Object3D in jPCT would.
     */
    public ClientObject() {
        super(0);
        touch();
    }

    /**
     * Create a new client object based on an existing Object3D.
     * @param obj the base object
     */
    public ClientObject(Object3D obj) {
        super(obj);
        ShaderProvider.setShader("phong", this);
        touch();
        if (Globals.compiledObjects) {
			compile(Globals.allDynamic);
		}}

    /**
     * Create a new client object based on an existing Object3D.
     * @param obj the base object
     */
    public ClientObject(Object3D obj, boolean reuseMesh) {
        super(obj, reuseMesh);
        ShaderProvider.setShader("phong", this);
        touch();
        if (Globals.compiledObjects) {
        	if (reuseMesh) {
        		shareCompiledData(obj);
        	}
			compile(Globals.allDynamic);
		}
    }

    
    /**
     * Creates a new client object plus a child object. This is a convience method for handling weapons
     * in this "game". The fact that the actual object is composed of parent and child is reflected in
     * some methods, not in all. Basically, the behaviour of the composed object is similar to an Object3D
     * in jPCT that has a child object attached to it.
     * @param obj the base object
     * @param child the base object of the child
     */
    public ClientObject(Object3D obj, Object3D child, boolean reuseMesh) {
        super(obj, reuseMesh);
        ShaderProvider.setShader("phong", this);
        addChild(child);
        if (reuseMesh) {
        	getChild().setMesh(child.getMesh());
        }
        if (Globals.compiledObjects) {
        	if (reuseMesh) {
        		shareCompiledData(obj);
        		getChild().shareCompiledData(child);
        	}
			compile(Globals.allDynamic);
			getChild().compile(Globals.allDynamic);
        }
    }
    
    public ClientObject(Object3D obj, Object3D child) {
        this(obj, child, false);
    }
    
    /**
     * Sets the model for the view. This is a cyclic reference as the model also knows its view (if any).
     * This is ugly, but the collision listener for the bombs needs access to the players model...there
     * is no way around this...:-(
     * @param obj
     */
    public void setModel(LocalObject obj) {
    	this.model=obj;
    }
    
    public LocalObject getModel() {
    	return model;
    }
    
    public void addChild(Object3D child) {
    	this.child = new ChildObject3D(child);
        super.addChild(this.child);
        touch();
    }
    
    public void enterCollisionMode() {
    	if (colMesh!=null && getVisibility()) {
    		collisionMode=true;
    		setVisibility(false);
    		colMesh.setVisibility(true);
    	}
    }
    
    public void leaveCollisionMode() {
    	if (colMesh!=null && collisionMode) {
    		collisionMode=false;
    		setVisibility(true);
    		colMesh.setVisibility(Globals.showCollisionMesh);
    	}
    }
    
    public void setCollisionMode(int mode) {
    	super.setCollisionMode(mode);
    	if (colMesh!=null && !Globals.showCollisionMesh) {
    		colMesh.setCollisionMode(mode);
    	}
    }
    
    public void setCollisionMesh(CollisionMesh mesh) {
    	this.colMesh=mesh;
    	super.addChild(colMesh);
    	mesh.setVisibility(Globals.showCollisionMesh);
    }
    
    /**
     * Do something special based on some object
     * @param obj
     */
    public void processSpecial(Object obj) {}
    
	public void hits(CollisionParticipant target, LocalObject obj, DecalManager decal, CollisionEvent ce) {
		// do nothing
	}
	
	public void hitByExplosion(CollisionParticipant source, LocalObject obj, DecalManager decal, CollisionEvent ce) {
		// do nothing
	}
	
	public void hitByLocalPlayer(CollisionParticipant source, LocalObject obj, DecalManager decal, CollisionEvent ce) {
		// do nothing
	}
	
	public void setClientID(int id) {
		// Optional...not every ClientObject has to set this. The PlayerDummy does...
		clientID=id;
	}
	
	public int getClientID() {
		return clientID;
	}
	
	public void setObjectID(int id) {
		// Optional...not every ClientObject has to set this. The PlayerDummy does...
		objectID=id;
	}
	
	public int getObjectID() {
		return objectID;
	}

	public void setEventQueue(ClientEventQueue queue) {
		this.eventQueue=queue;
	}
	
	public ClientEventQueue getEventQueue() {
		return eventQueue;
	}
	

    /**
     * In this default implementation, the method does nothing. You may fill it for a client object
     * if you want to process something that is based on a local object that has been created by this client and
     * that should be treated different from what process() itself does.
     * @param ticks time passed since the last run
     * @param level the current level
     * @param lo the LocalObject that this client object represents. Client objects and local objects are not
     * bound directly, which is why the client object doesn't know of its local one.
     */
    public void processLocal(long ticks, Level level, LocalObject lo) {
    }

    /**
     * In this default implementation, the method does nothing. This is for all kinds of processing like playing
     * animation, doing the movement etc...
     * @param ticks time passed since the last run
     * @param level the current level
     */
    public void process(long ticks, Level level) {
    }
    
    public void processForBotClient(long ticks, Level level) {
    	process(ticks, level); // Defaults to process. May be overridden to change this
    }

    public Object3D getChild() {
    	return child;
    }
    
    public void addChildToSuper(Object3D child) {
    	super.addChild(child);
    }
    
    /**
     * Adds the object as well as a child (if any) to the world. This is similar to World.addObject() from the
     * jPCT framework but it takes care of the child object.
     * @param world the world
     */
    public void addToWorld(World world) {
        world.addObject(this);
        if (child != null) {
            world.addObject(child);
        }
        if (colMesh!=null) {
        	world.addObject(colMesh);
        }
    }

    public void initTransformation() {
    	SimpleVector pos = getBackPosition();
        getTranslationMatrix().setIdentity();
        translate(pos);
        //Matrix m=getBackRotationMatrix();
        getRotationMatrix().setTo(getBackRotationMatrix());
        //setRotationMatrix(m.cloneMatrix());
    }
    
    /**
     * Removes the object as well as a child (if any) from the world. This is similar to World.removeObject() from the
     * jPCT framework but it takes care of the child object.
     * @param world the world
     */
    public void removeFromWorld(World world) {
    	try {
        world.removeObject(this);
	        if (child != null) {
	            world.removeObject(child);
	        }
	        if (colMesh!=null) {
	        	world.removeObject(colMesh);
	        }
    	} catch(Exception e) {
    		// Very seldom, it happens that an already removed object should be removed due to some
    		// timing problems...we simply ignore the exception then
    		NetLogger.log("Object "+this.getID()+" already removed from the world!");
    	}
    }

    public void animate(float ix, int seq) {
    	// Synchronize the animations to the sequence. I had funny side-effects
    	// when the bot-client this to play an animation while the normal client was.
    	// The bot client shouldn't play animations at all, but...well...just to be sure...
    	synchronized(super.getAnimationSequence()) {
    		super.animate(ix, seq);
    	}
        if (child != null) {
        	if (child.getAnimationSequence().getSequenceCount()>=seq) {
        		if (!child.getVisibility()) {
        			child.setRealVisibility(true);
        		}
        		childAnimVisible=true;
        		synchronized(child.getAnimationSequence()) {
        			child.animate(ix, seq);
        		}
        	} else {
        		child.setRealVisibility(false);
        		childAnimVisible=false;
        	}
        }
    }

    public void setSuperVisibility(boolean visi) {
    	super.setVisibility(visi);
    }
    
    public boolean getSuperVisibility() {
    	return super.getVisibility();
    }
    
    public boolean getVisibility() {
    	boolean res=super.getVisibility();
    	if (child!=null) {
    		res|=child.getVisibility();
    	}
		return res;
	}
    
    public void setVisibility(boolean visi) {
        super.setVisibility(visi);
        if (child != null && childAnimVisible) {
            child.setRealVisibility(visi);
        }
    }

    /**
     * A short cut to set the clamping mode for this object's animation (and it child object).
     * @param mode the new clamping mode like in jPCT's Animation class.
     */
    public void setClampingMode(int mode) {
    	if (super.getAnimationSequence()!=null) {
	        super.getAnimationSequence().setClampingMode(mode);
	        if (child != null) {
	            child.getAnimationSequence().setClampingMode(mode);
	        }
    	}
    }

    /**
     * Determines if the object is "old". In this context, an object is old if it hasn't been
     * touched for more than 5 seconds.
     * @return boolean is it old?
     */
    public boolean isOld() {
        return !localOnly && (Ticker.hasPassed(lastAccess, 5000));
    }

    /**
     * "Touches" the object, i.e. it marks the object as being used with a timestamp.
     */
    public void touch() {
        lastAccess = Ticker.getTime();
    }

    public void setRemoteFlag(boolean flag) {
        isRemote = flag;
    }
    
    public void setTransfered(boolean is) {
    	isTransfered=is;
    }
    
    public boolean isTransfered() {
    	return isTransfered;
    }

    /**
     * Returns if this object is a remote one or a local one.
     * @return boolean is it remote?
     */
    public boolean isRemote() {
        return isRemote;
    }
    
    /**
     * Marks this object has being modified. Modified means, that the server has updated the local object which
     * this object represents.
     * @param mod true, if it has been modified
     */
    public void setModified(boolean mod) {
        isModified = mod;
    }

    /**
     * Returns if this object has been modified. Modified means, that the server has updated the local object which
     * this object represents.
     * @return boolean is it modified?
     */
    public boolean isModified() {
        return isModified;
    }

    /**
     * Sets the back rotation matrix. This matrix has been changed due to a change in the server's data but that change
     * hasn't been reflected in the view yet.
     * @param mat the new rotation matrix
     */
    public void setBackRotationMatrix(Matrix mat) {
        backRot = mat;
    }

    /**
     * Returns the back rotation matrix.
     * @return Matrix the matrix
     */
    public Matrix getBackRotationMatrix() {
        return backRot;
    }

    /**
     * Sets the back position. This position has been changed due to a change in the server's data but that change
     * hasn't been reflected in the view yet.
     * @param pos the new position
     */
    public void setBackPosition(SimpleVector pos) {
        backPos = pos;
    }

    /**
     * Returns the back position.
     * @return SimpleVector the position
     */
    public SimpleVector getBackPosition() {
        return backPos;
    }

    /**
     * Sets the back speed vector. This speed vector has been changed due to a change in the server's data but that change
     * hasn't been reflected in the view yet.
     * @param spd the new speed vector
     */
    public void setBackSpeed(SimpleVector spd) {
        speed = spd;
    }

    /**
     * Returns the nback speed vector.
     * @return SimpleVector the vector
     */
    public SimpleVector getBackSpeed() {
        return speed;
    }

    /**
     * Updates the backbuffer of this object with data from a LocalObject. Usually, this object has been
     * transfered from the server and contains a new position, speed etc...
     * @param lo the local object that contains the new data
     */
    public void setToLocalObject(LocalObject lo) {
    	
    	if (!lo.getRotation().equals(this.getBackRotationMatrix()) || !lo.getPosition().equals(getBackPosition()) || !lo.getSpeed().equals(getBackSpeed())
    			|| lo.getAnimation()!=getBackAnimation()) {
    		setBackRotationMatrix(lo.getRotation());
            setBackPosition(lo.getPosition());
            setBackSpeed(lo.getSpeed());
            setBackAnimation(lo.getAnimation());
            setModified(true);
    	} /*else {
    		if (lo.getType()==Types.PLAYER && !getBackSpeed().equals(SimpleVector.ORIGIN)) {
    			System.out.println(getBackSpeed()+"/"+lo.getRotation()+"/"+lo.getPosition()+"/"+lo.getSpeed()+"/"+lo.getAnimation());
    		}
    		}*/
    	
        setBackAnimationSpeed(lo.getAnimationSpeed());
        setBackValue(lo.getValue());
    }
    
    public int getBackValue() {
    	return value;
    }
    
    public void setBackValue(int val) {
    	value=val;
    }

    /**
     * Resets the animation ticks. A client object doesn't necessarily use an animation, but the LocalObject
     * itself contains the information. Therefore, the basic animation methods are within this class, even if
     * it may not use them.
     */
    public void resetTicks() {
        ticks = 0;
    }

    /**
     * Gets the animation ticks passed.
     * @return long the ticks
     */
    public long getTicks() {
        return ticks;
    }

    /**
     * Adds a number of ticks to the current tick count.
     * @param val the number of ticks to add
     */
    public void addToTicks(long val) {
        ticks += val;
        if (ticks < 0) {
            ticks = lastAnimSpeed - (( -ticks) % lastAnimSpeed);
        }
    }

    /**
     * Sets the last animation sequence. This is the one that has been used in the last displayed frame.
     * @param seq the sequence
     */
    public void setLastSequence(int seq) {
        lastSequence = seq;
    }

    /**
     * Returns the last animation sequence. This is the one that has been used in the last displayed frame.
     * @return int the sequence
     */
    public int getLastSequence() {
        return lastSequence;
    }

    /**
     * Sets the animation speed of the currently displayed animation.
     * @param speed the speed
     */
    public void setAnimSpeed(int speed) {
        lastAnimSpeed = speed;
    }

    /**
     * Sets the animtion speed of the backbuffer animation.
     * @param speed the speed
     */
    public void setBackAnimationSpeed(int speed) {
        backAnimSpeed = speed;
    }

    /**
     * Gets the speed of the backbuffer animation.
     * @return int the speed
     */
    public int getBackAnimationSpeed() {
        return backAnimSpeed;
    }

    /**
     * Sets the backbuffer animation sequence.
     * @param anim the sequence number
     */
    public void setBackAnimation(int anim) {
        backAnim = anim;
    }

    /**
     * Returns the backbuffer animation sequence.
     * @return int the sequence number
     */
    public int getBackAnimation() {
        return backAnim;
    }
    
    public void setAsLocalOnly() {
    	localOnly=true;
    }
    
    public boolean isLocalOnly() {
    	return localOnly;
    }
    
    static class ChildObject3D extends Object3D {
    	
    	protected static final long serialVersionUID=1;
    	
    	public ChildObject3D(Object3D obj) {
    		super(obj);
    	}
    	
    	public void setVisibility(boolean visi) {
    		// Do Nothing, the parent will handle this!
    	}
    	
    	public void setRealVisibility(boolean visi) {
    		super.setVisibility(visi);
    	}
    	
    }
    
}
