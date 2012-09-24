package robombs.game.model;

import robombs.game.*;
import robombs.game.util.*;

import com.threed.jpct.*;
import robombs.game.sound.*;

/**
 * A local object that holds a player's data.
 */
public class LocalPlayerObject extends LocalObject {

    private Matrix viewRot = new Matrix();
    private SimpleVector ellipsoid = new SimpleVector(3.5f, 5, 3.5f);
    private float turnSpeed = 0;

    protected float moveSpeed = 0.75f;
    private boolean isDead = false;
    private long time = 0;
    private boolean isActive = false;
    private PlayerPowers powers=null;
    private float zoomFactor=1;
    private long invTime = 0;
    
    private SimpleVector movement=null;
    
    public LocalPlayerObject(int clientID) {
    	super(clientID);
    	setSpecialValue(Globals.LIVES);
    	powers=new PlayerPowers();
    }
    
    public LocalPlayerObject() {
    	cid=0;
    	powers=new PlayerPowers();
    }
    
    public void setClientID(int id) {
    	cid=id;
    }
    
    public void reset() {
    	setSpecialValue(Globals.LIVES);
    	powers=new PlayerPowers();
    	setViewRotation(new Matrix());
		setRotation(new Matrix());
		setSpeed(new SimpleVector());
		setType(Types.PLAYER);
		setPosition(new SimpleVector(0,-10000,0));
		setAlive();
    }
    
    public void setTurnSpeed(float speed) {
    	turnSpeed=speed;
    }
    
    public PlayerPowers getPlayerPowers() {
    	return powers;
    }
    
    public SimpleVector getEllipsoid() {
    	return ellipsoid;
    }
    
    /**
     * Gets the "view rotation". This is the direction in which the player is looking. Not the direction
     * in which he/she's moving.
     * @return Matrix the view rotation
     */
    public Matrix getViewRotation() {
        return viewRot;
    }

    /**
     * Sets the "view rotation". This is the direction in which the player is looking. Not the direction
     * in which he/she's moving.
     * @param mat the view direction
     */
    public void setViewRotation(Matrix mat) {
        viewRot = mat;
    }
    
    public boolean shouldbeTransparent() {
    	return zoomFactor<=0.35f;
    }

    /**
     * Flag the player as dead.
     */
    public synchronized void setDead() {
        if (!isDead) {
        	setValue(0);
            isDead = true;
            time = Ticker.getTime();
            long spec=getSpecialValue();
            if (spec>0) { 
                spec--; 
                setSpecialValue(spec); 
            } else { 
            	setSpecialValue(0); // Just to make sure that's never below 0 for some reason        
            } 
            if (this.getView()!=null) {
            	// Make sure that the coarpse doesn't move on other clients
            	setSpeed(SimpleVector.ORIGIN);
            }
        }
    }

    /**
     * Flag the player as alive.
     */
    public synchronized void setAlive() {
        if (isDead) {
            isDead = false;
            time = 0;
            setValue(100);
            getPlayerPowers().refillWater();
        }
    }

    /**
     * Is the player dead?
     * @return boolean is he/she?
     */
    public synchronized boolean isDead() {
        return isDead;
    }
    
    /**
     * Is the player is dead, here's since how long...
     * @return long how long is the player dead?
     */
    public long deadSince() {
        return time;
    }
    
    public SimpleVector getLocalMovementCorrection() {
    	return movement;
    }

    public void setLocalMovementCorrection(SimpleVector mc) {
    	if (mc!=null) {
    		movement=new SimpleVector(mc);
    	} else {
    		movement=null;
    	}
    }
    
    /**
     * Activate a player. An active player is currently playing but can very well be dead.
     */
    public void activate() {
        isActive = true;
    }

    /**
     * Deactivate a player.
     */
    public void deactivate() {
        isActive = false;
    }


    /**
     * Is the player active. An active player is currently playing but can very well be dead.
     * @return boolean is the player active?
     */
    public boolean isActive() {
        return isActive;
    }

    /**
     * Align the camera to the player.
     * @param camera the camera that should be aligned
     */
    public void alignCamera(Camera camera, int ticks) {
        if (getPosition() != null) {
            if (!isDead() || !isActive()) {
            	
        	    for (int i=0; i<ticks; i++) {
        	    	// Making this interpolation based on ticks fixes the jitter on low fps
                	SimpleVector center=getPosition();
            	    
        	        SimpleVector oldCamPos=camera.getPosition();
        	        SimpleVector oldestCamPos=new SimpleVector(oldCamPos);
        	        oldCamPos.scalarMul(4f);

        	        SimpleVector camPos=new SimpleVector(center);
        	        SimpleVector zOffset=getViewRotation().invert3x3().getZAxis();
        	        SimpleVector yOffset=new SimpleVector(0, -50*zoomFactor*zoomFactor, 0);
        	        zOffset.scalarMul(-70f*zoomFactor);

        	        camPos.add(zOffset);
        	        camPos.add(yOffset);

        	        camPos.add(oldCamPos);
        	        camPos.scalarMul(0.2f);

        	        SimpleVector delta=camPos.calcSub(oldestCamPos);
        	        float len=delta.length();

        	        if (len!=0) {
        	           camera.moveCamera(delta.normalize(), len);
        	        }

        	        camera.lookAt(center);
        	    }
            }
        }
    }

    /**
     * Move the player according to key and mouse input.
     * @param world the world in which the player exists
     * @param keys the currently pressed keys
     * @param mouse the mouse input
     * @param ticks the game time passed since the last call
     * @param a dummy object used for collision detection. With this, the listener can distinguish between
     * collision with no source (explosions) and the player.
     * @return boolean true, if the player has moved. False otherwise.
     */
    public boolean move(World world, KeyStates keys, MouseMapper mouse, long ticks, PlayerDummy dummy, int forcedStepsBack) {
        // Should be possible to use the player instead of dummy...todo!
    	
    	PlayerPowers pp=getPlayerPowers();
    	int sick=pp.isSick();
    	float mul=1;
    	if (sick==PlayerPowers.SLOWER) {
    		mul=0.7f;
    	}
    	if (sick==PlayerPowers.FASTER) {
    		mul=1.3f;
    	}
    	
        SimpleVector pos = getPosition();
        boolean changed = false;
        if (!isDead() && this.getAnimation()!=Animations.DIE && this.getAnimation()!=Animations.DEAD) {
	        // Don't do this, if either the pos is null (unlikely) or the y-position is very low,
        	// which means that the player doesn't touch the ground but floats in the sky.
        	if (pos != null && pos.y>Globals.skyLimit) {
	        	pos.y=-10.01f;
	
	            setSpeed(new SimpleVector());
	            pos.add(new SimpleVector(0, ellipsoid.y, 0));
	
	            SimpleVector netSpeed=new SimpleVector();
	            SimpleVector temp=new SimpleVector();
	            
	            if (KeyStates.up && forcedStepsBack<=0) {
	                changed = true;
	                temp.set(getRotation().getZAxis());
	            }
	
	            if (KeyStates.down || forcedStepsBack>0) {
	                changed = true;
	                temp.set(getRotation().getZAxis());
	                temp.scalarMul(-1);
	            }
	
	            if (KeyStates.left) {
	                changed = true;
	                SimpleVector temp2=getRotation().getXAxis();
	                temp2.scalarMul(-1);
	                temp.add(temp2);
	                
	            }
	
	            if (KeyStates.right) {
	                changed = true;
	                SimpleVector temp2=getRotation().getXAxis();
	                temp.add(temp2);
	            }
	            
	            if (getLocalMovementCorrection()!=null) {
	            	// When entering the collision mode of the bombs, the player get checked
	            	// if he's located "inside" the bomb. If that is the case, an alternate
	            	// movement vector will be calculated and set. This is, what you see here.
	            	// This vector exists on the client only. It will never be transfered over the net.
	            	temp=getLocalMovementCorrection();
	            } else {
	            	temp=temp.normalize();
	            }
	            temp.scalarMul(mul*moveSpeed*(float) ticks);
	            dummy.getTranslationMatrix().setIdentity();
	            dummy.translate(pos);
	            temp = dummy.checkForCollisionEllipsoid(temp, ellipsoid, 7);
	            pos.add(temp);
	            temp.scalarMul(1f/(float)ticks);
	            netSpeed.add(temp);
	           
	            setSpeed(netSpeed);
	            
	            Matrix rot = getRotation();
	
	            int dx = mouse.getDeltaX();
	
	            float ts = 0;
	            float angle=SoundManager.ANGLE_NOT_CHANGED;
	
	            if (dx != 0) {
	                ts = Math.abs(dx) / -(((400-turnSpeed)+1)*10);
	                angle=ts;
	            }
	
	            if (dx < 0) {
	                viewRot.rotateAxis(viewRot.getYAxis(), ts);
	                rot.rotateY(ts);
	            }
	
	            if (dx > 0) {
	                viewRot.rotateAxis(viewRot.getYAxis(), -ts);
	                rot.rotateY( -ts);
	            }
	            
	            dummy.setBackRotationMatrix(rot); // Store it in the dummy...collision detection needs it
	
	            zoomFactor-=0.125f*(float) mouse.getWheel();
	            if (zoomFactor<Globals.minZoom) {
	            	zoomFactor=Globals.minZoom;
	            }
	            if (zoomFactor>Globals.maxZoom) {
	            	zoomFactor=Globals.maxZoom;
	            }
	            
	            pos.add(new SimpleVector(0, -ellipsoid.y, 0));
	            pos.y=-10.01f;
	            setPosition(pos);
	            
	            SoundManager.getInstance().setListener(pos, angle, ticks);
	        }
        } else {
        	// Nothing moves here anymore...
        	setSpeed(new SimpleVector());
        }
        return changed;
    }
    
    public void setInvincible(boolean inv) {
    	if (inv) {
    		invTime=Ticker.getTime();
    	}
    	super.setInvincible(inv);
    }
    
    public boolean isInvincible() {
    	boolean ret=super.isInvincible();
    	if (ret) {
    		if (Ticker.hasPassed(invTime, Globals.invincibleTime)) {
    			ret=false;
    			super.setInvincible(false);
    		}
    	}
    	return ret;
    }
    
}
