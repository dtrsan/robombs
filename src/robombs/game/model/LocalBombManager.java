package robombs.game.model;

import java.util.*;

import robombs.game.*;
import robombs.clientserver.*;
import robombs.game.view.*;
import robombs.game.util.*;
import robombs.game.ai.*;

import com.threed.jpct.*;
import com.threed.jpct.util.*;

public class LocalBombManager extends LocalObjectManager {
	
	private final static Matrix DUMMY_MAT=new Matrix();
	private final static SimpleVector DUMMY_SPEED=new SimpleVector();
	private final static SimpleVector BOMB_OFFSET=new SimpleVector(0,5.5f,0);
	
	private SimpleVector ellipsoidStart = new SimpleVector(BombView.RADIUS-1.5f, BombView.RADIUS-1.5f, BombView.RADIUS-1.5f);
	
	public final static long LIFE_TIME=2000;
	public final static float ACTIVATION_DISTANCE=3.5f;
	
	public final static int VALUE_INACTIVE=1;
	public final static int VALUE_ACTIVE=2;
	public final static int VALUE_EXPLODING=3;
	public final static int VALUE_DISABLED=4;
	
	public final static int DURATION_MUL=15;
	
	private MapMask bombMask=null;
	private long lifeTime=LIFE_TIME;
	
	private ShadowHelper sh=null;
	private int count=0;
	
	/**
     * Creates a new manager.
     */
    public LocalBombManager(Level level, ShadowHelper shadower) {
    	bombMask=new MapMask(level.getWidth(), level.getHeight());
    	sh=shadower;
    	level.setLocalBombManager(this);
    }
    
    public void setLifeTime(long time) {
    	lifeTime=time;
    }
    
    public MapMask getBombMask() {
    	return bombMask;
    }
    
    public LocalObject getLocalBombAt(GridPosition gp) {
    	for (LocalObject bomb : objs) {
    		SimpleVector pos=bomb.getPosition();
    		GridPosition gpb=bombMask.getGrid(pos.x, pos.z);
    		if (gp.equals(gpb)) {
    			return (LocalBomb) bomb;
    		}
    	}
    	return null;
    }
    
    /**
     * Adds a new bomb to the manager and, if not null, a new view for that bomb to the world.
     * @param player the player which places the bomb
     * @param world the world in which the player exists
     * @return LocalObject the new bomb model
     */
    public synchronized LocalObject addBomb(SimpleClient client, LocalPlayerObject player, World world, ClientEventQueue eventQueue) {
        
    	SimpleVector pos=new SimpleVector(player.getPosition());
        pos.add(BOMB_OFFSET);
        GridPosition gridPos=bombMask.getGrid(pos.x, pos.z);
        
        if (bombMask.getMaskAt(gridPos)!=MapMask.BOMB && !isTooClose(pos)) {
        	
        	if (Globals.useBombingGrid) {
        		// Correct the bomb's position slightly to allow for a more "dynamic" game play
        		SimpleVector gp=gridPos.convertTo3D();
        		float dx=gp.x-pos.x;
        		float dz=gp.z-pos.z;
        		
        		if (dx>=-Globals.bombingGridWidth && dx<-2.9f) {
        			pos.x+=(dx+2.9f);
        			//System.out.println("x corrected to "+pos.x+" by "+(dx+3));
        		}
        		
        		if (dx<=Globals.bombingGridWidth && dx>2.9f) {
           		 	pos.x+=(dx-2.9f);
           		 	//System.out.println("x corrected to "+pos.x+" by "+(dx-3));
           		}
        		
        		if (dz>=-Globals.bombingGridWidth && dz<-2.9f) {
           		 	pos.z+=(dz+2.9f);
           		 	//System.out.println("z corrected to "+pos.z+" by "+(dz+3));
           		}
        		
        		if (dz<=Globals.bombingGridWidth && dz>2.9f) {
        			pos.z-=(dz-2.9f);
        			//System.out.println("z corrected to "+pos.z+" by "+(dz+3));
              	}
        	}
        	gridPos=bombMask.getGrid(pos.x, pos.z); // Correct the grid. It may have changed.
        	bombMask.setMaskAt(gridPos, MapMask.BOMB);
        	
	        LocalBomb bomb=new LocalBomb(client.getClientID());
	        bomb.setPosition(pos);
	        bomb.setType(Types.BOMB);
	        
	        Matrix rot=DUMMY_MAT;
	        //float yRot=(float)Math.random()*3f;
	        //rot.rotateY(yRot);
	        
            bomb.setSpecialValue(player.getObjectID());
	        bomb.setRotation(rot);
	        bomb.setSpeed(DUMMY_SPEED);
	        bomb.setValue(VALUE_INACTIVE);
	        bomb.setAnimationSpeed(player.getPlayerPowers().getFirePower()); // This is the max length of the blow!
	
	        if (world!=null) {
	            ClientObject b=ClientObjectFactory.getInstance().create(Types.BOMB);
	            b.setEventQueue(eventQueue);
	           // b.rotateY(yRot);
	            b.setBackAnimationSpeed(bomb.getAnimationSpeed()); // This is the max length of the blow in the view!
	            b.setToLocalObject(bomb);
	            b.addToWorld(world);
	            bomb.setView(b);
	            b.getTranslationMatrix().setIdentity();
	            b.translate(bomb.getPosition());
	            b.addCollisionListener(new ClientObjectCollisionListener(bomb, true));
	            if (sh!=null) {
	            	sh.addCaster(b);
	            }
	            count++;
	        }
	        objs.add(bomb);
	        return bomb;
        } 
        return null;
    }
    
    public boolean isTooClose(SimpleVector pos) {
    	for (LocalObject bomb : objs) {
    		if (bomb.getPosition().calcSub(pos).length()<BombView.RADIUS*3f) {
    			return true;
    		}
    	}
    	return false;
    }
    
    public LocalObject getLocalObject(int viewId) {
    	for (LocalObject bomb : objs) {
    		if (bomb.getView()!=null && bomb.getObjectID()==viewId) {
    			return bomb;
    		}
    	}
    	return null;
    }
    
    public void setShadowHelper(ShadowHelper sh) {
    	this.sh=sh;
    }

    public void moveBombs(World world, long ticks) {
    	for (LocalObject bomb : objs) {
    		LocalBomb lb=(LocalBomb) bomb; // This cast is save!
    		if (bomb.getValue()!=VALUE_INACTIVE && bomb.getValue()!=VALUE_EXPLODING) {
    			if (lb.isMoving() && lb.getView()!=null) {
    				SimpleVector speed=new SimpleVector(lb.getSpeed());
    				speed.scalarMul(ticks);
    	            SimpleVector temp = world.checkCollisionEllipsoid(lb.getPosition(), speed, ellipsoidStart, 2);
    	            if (temp.length()/ticks<(Globals.bulletSpeed*2f)-1) {
    	            	lb.stop();
    	            	lb.setSpeed(SimpleVector.ORIGIN);
    	            	lb.setValue(VALUE_ACTIVE);
    	            	
    	            	// The collision radius is smaller than the explosion...correct this by using a collision
    	            	// detection algo instead of a collision avoidance one. The spherical approach pushes you
    	            	// out of collisions.
    	            	temp = world.checkCollisionSpherical(lb.getPosition(), SimpleVector.ORIGIN, BombView.RADIUS);
    	            	temp.y=0;
    	            	SimpleVector pos=lb.getPosition();
    	            	pos.add(temp);
    	            	lb.setPosition(pos);
    	            	lb.setLocalTimeStamp(0); //BOOM!
    	            } else {
    	            	temp.y=0;
    	            	SimpleVector pos=lb.getPosition();
    	            	pos.add(temp);
    	            	lb.setPosition(pos);
    	            	temp.scalarMul(1f/(float) ticks);
        	            lb.setSpeed(temp);
        	            lb.getRotation().rotateY(0.5f*ticks);
    	            }
    			}
    		} else {
    			lb.stop();
    			lb.setSpeed(SimpleVector.ORIGIN);
    		}
    	}
    }

    
    public void processLocalBombs(LocalPlayerObject player, Level level, long ticks) {
    	for (LocalObject bomb : objs) {
    		if (bomb.getValue()==VALUE_INACTIVE) {
	    		SimpleVector pos=new SimpleVector(player.getPosition());
	    		pos.y=0;
	    		SimpleVector posBomb=new SimpleVector(bomb.getPosition());
	    		posBomb.y=0;
	    		float length=pos.calcSub(posBomb).length();
	    		if (length>=BombView.RADIUS+ACTIVATION_DISTANCE || player.isDead()) {
	    			bomb.setValue(VALUE_ACTIVE);
	    			bomb.setLocalTimeStamp(Ticker.getTime());
	    		}
    		} else {
    			if (bomb.getValue()==VALUE_ACTIVE && Ticker.getTime()-bomb.getLocalTimeStamp()>=lifeTime) {
    				bomb.setValue(VALUE_EXPLODING);
    				bomb.setLocalTimeStamp(Ticker.getTime());
    			}
    		}	
    		
    		ClientObject bv = bomb.getView();
			if (bv != null) {
				bv.setToLocalObject(bomb);
				bv.process(ticks, level);
			}
    	}
    }
    
    /**
     * Sets the visibility for local bombs. Used to disable collision detection of the bots on their own
     * bombs for a short time to allow them to escape without getting stuck.
     * @param playerID
     * @param maxTime
     * @param visible
     */
    public void setVisibilityOfBombs(int playerID, long maxTime, boolean visible) {
    	for (LocalObject bomb : objs) {
    		if (bomb.getValue()==VALUE_ACTIVE || bomb.getValue()==VALUE_DISABLED) {
    			if (bomb.getSpecialValue()==playerID && (visible || Ticker.hasNotPassed(bomb.getLocalTimeStamp(), maxTime))) {
    				ClientObject co=bomb.getView();
    				if (co!=null) {
    					co.setVisibility(visible);
    				}
    			}
    		}
    	}
    }
    
   
    
    public void enterCollisionMode() {
    	 enterCollisionMode(null);
    }
    
    public void enterCollisionMode(LocalPlayerObject player) {
     if (player!=null) {
    	 player.setLocalMovementCorrection(null);
     }
   	 for (LocalObject bomb : objs) {
        BombView co=(BombView)bomb.getView(); // This cast is save!
   		co.enterCollisionMode(player);
   	 }
   }
    
    public void leaveCollisionMode() {
    	for (LocalObject bomb : objs) {
            ClientObject co=bomb.getView();
    		co.leaveCollisionMode();
    	}
    }
    
    public void processBotBombs(List<Bot> bots, Level level, long ticks) {
    	for (LocalObject bomb : objs) {
    		if (bomb.getValue()==VALUE_INACTIVE) {
    			boolean found=false;
    			for (Bot bot:bots) {
		    		SimpleVector pos=new SimpleVector(bot.getPosition());
		    		pos.y=0;
		    		SimpleVector posBomb=new SimpleVector(bomb.getPosition());
		    		posBomb.y=0;
		    		float length=pos.calcSub(posBomb).length();
		    		if (length<BombView.RADIUS+ACTIVATION_DISTANCE+0.25f && !bot.isDead()) {
		    			found=true;
		    			break;
		    		}
    			}
    			if (!found) {
    				bomb.setValue(VALUE_ACTIVE);
	    			bomb.setLocalTimeStamp(Ticker.getTime());
    			}
    		} else {
    			if (bomb.getValue()==VALUE_ACTIVE && Ticker.getTime()-bomb.getLocalTimeStamp()>=lifeTime) {
    				bomb.setValue(VALUE_EXPLODING);
    				bomb.setLocalTimeStamp(Ticker.getTime());
    			}
    		}
    		
    		ClientObject bv = bomb.getView();
			if (bv != null) {
				bv.setToLocalObject(bomb);
				bv.processForBotClient(ticks, level);
			}
			
    	}
    }
    
    public void explode(int id) {
    	LocalObject lo = null;
		lo = getLocalObject(id);
		if (lo != null) {
			
			/*
			// it happens from time to time, that a bomb doesn't get cleared from the mask...
			// ...no idea why. Maybe this will fix it:
			
			SimpleVector pos=lo.getPosition();
			GridPosition gridPos=bombMask.getGrid(pos.x, pos.z);
			bombMask.setMaskAt(gridPos, MapMask.NO_BOMB);
			*/
			
			lo.setSpeed(SimpleVector.ORIGIN);
			
			if (lo.getValue()!=LocalBombManager.VALUE_EXPLODING) {
				// Do this only if the bomb isn't exploding yet...
				lo.setLocalTimeStamp(0); // Das lässt die Bombe platzen! :-)
				if (lo.getValue()==LocalBombManager.VALUE_DISABLED) {
					lo.setValue(LocalBombManager.VALUE_ACTIVE);
				}
			}
		} else {
			NetLogger.log("Unable to explode - local bomb already removed!");
		}
    }
    
    public boolean startMoving(int id, Event event) {
    	SimpleVector direction= event.getDirection();
    	LocalObject lo = null;
		lo = getLocalObject(id);
		if (lo != null && !((LocalBomb)lo).isMoving()) {
			if (lo.getValue()==LocalBombManager.VALUE_ACTIVE || lo.getValue()==LocalBombManager.VALUE_DISABLED) {
				((LocalBomb)lo).setDirection(direction);
				lo.setRotation(new Matrix(lo.getRotation()));
				if (lo.getView()!=null) {
					// There has to be a view...
					lo.getView().setCollisionMode(Object3D.COLLISION_CHECK_OTHERS | Object3D.COLLISION_CHECK_SELF);
					lo.getView().setRotationMatrix(new Matrix(lo.getView().getRotationMatrix()));
					return true;
				}
			}
		}
		return false;
    }
    
    public void defuse(int id) {
    	LocalObject lo = null;
		lo = getLocalObject(id);
		if (lo != null) {
			if (lo.getValue()==LocalBombManager.VALUE_ACTIVE) {
				// Do this only if the bomb is active ATM...
				lo.setValue(LocalBombManager.VALUE_DISABLED);
				lo.disable();
				count--;
			}
		} 
    }
    
    public int getCount() {
    	return count;
    }
    
    public int getCount(int objectID) {
      int cnt=0;
      for (Iterator<LocalObject> itty=objs.iterator(); itty.hasNext();) {
        LocalObject bomb=itty.next();
        if (bomb.getSpecialValue()==objectID) {
          cnt++;
        }
      }
      return cnt;
    }
    
    public synchronized List<LocalObject> fillOrKill(ExtendedDataContainer dc, ClientEventQueue eventQueue) {
		List<LocalObject> toKill = new ArrayList<LocalObject>();
		for (Iterator<LocalObject> itty=objs.iterator(); itty.hasNext();) {
			LocalObject bomb=itty.next();
			if (bomb.getValue()==VALUE_EXPLODING && Ticker.hasPassed(bomb.getLocalTimeStamp(), bomb.getAnimationSpeed()*DURATION_MUL)) {
				// Explosion ended? Get rid of it!
				if (sh!=null) {
					sh.removeCaster(bomb.getView());
				}
				itty.remove();
				toKill.add(bomb);
				Event ev = new Event(Event.ENTITY_REMOVE, bomb, bomb);
				ev.setOrigin(bomb.getPosition());
				eventQueue.add(ev);
				SimpleVector pos=bomb.getPosition();
				GridPosition gridPos=bombMask.getGrid(pos.x, pos.z);
				bombMask.setMaskAt(gridPos, MapMask.NO_BOMB);
				if (bomb.getView()!=null && !bomb.isDisabled()) {
					// Actually, there has to be a view...but just to be sure.
					// This is another reading for: "The bomb has been placed by the local client".
					count--;
				}
			}
			dc.add(bomb);
		}
		return toKill;
	}
}
