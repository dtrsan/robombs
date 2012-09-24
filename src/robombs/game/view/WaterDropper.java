package robombs.game.view;

import java.awt.Color;
import java.util.Collection;

import robombs.game.Event;
import robombs.game.Globals;
import robombs.game.model.*;
import robombs.game.*;

import com.threed.jpct.*;

public class WaterDropper {

	public final static Color ADD_COLOR=new Color(130,130,160);
	
	int cnt=0;
	
	public SimpleVector getInitialPosition(LocalPlayerObject player) {
		SimpleVector pos = new SimpleVector(player.getPosition());
		Matrix rot = player.getViewRotation().invert3x3();
		SimpleVector speed = rot.getZAxis();
		speed.scalarMul(2);
		SimpleVector addy = rot.getYAxis();
		addy.scalarMul(3.5f);
		pos.add(addy);
		addy = rot.getXAxis();
		addy.scalarMul(3);
		pos.add(addy);
		return pos;
	}
	
	public SimpleVector[] addDrop(LocalPlayerObject player, Level level) {
		SimpleVector pos = getInitialPosition(player);
		Matrix rot = player.getViewRotation().invert3x3();
		SimpleVector speed = rot.getZAxis();
		PlayerPowers pp=player.getPlayerPowers();
		speed.scalarMul(Globals.bulletSpeed);
		speed.scalarMul(0.4f+(float)Math.random()/4f);
		float sp=(float)Math.sqrt(((float)pp.getMaxWater()-(float)pp.getWater())/(float)pp.getMaxWater());
		speed.scalarMul((float)Math.max(0.35, sp));
		return addDrop(pos, speed, level, false);
	}
	
	public SimpleVector[] addDrop(SimpleVector pos, SimpleVector speed, Level level, boolean isRemote) {
		ParticleManager pm=level.getParticleManager();
		speed.y=-0.05f;
		if ((cnt&1)==1) {
			pm.addParticle(pos, speed, 10f, -1, "drop", 3f+(float)Math.random(), false, true, isRemote, ADD_COLOR);
		} else {
			pm.addParticle(pos, speed, 10f, -1, "drop2", 3f+(float)Math.random(), false, true, isRemote, ADD_COLOR);
		}
		cnt++;
		return new SimpleVector[] {pos, speed};
	}
	
	public void process(Level level, LocalBombManager lbm, ClientObjectManager com) {
		ParticleManager pm=level.getParticleManager();
		int cnt=pm.getCount();
		Particle[] parts=pm.getParticles();
		MapMask mask=level.getMask();
		MapMask bombMask=lbm.getBombMask();
		for (int i = 0; i < cnt; i++) {
	      Particle pp = parts[i];
	      if (pp!=null && pp.isWater() && pp.getVisibility()) {
	    	  SimpleVector pos=pp.getOrigin();
	    	  pos.add(pp.getTranslation());
	    	  GridPosition gp=mask.getGrid(pos.x, pos.z);
	    	  int xp=gp.getX();
	    	  int zp=gp.getZ();
	    	  if (mask.isObstacle(xp, zp) || mask.isBlocked(xp, zp)) {
	    		  pp.reset();
	    		  pp.setVisibility(Object3D.OBJ_INVISIBLE);
	    	  } else {
	    		  // To implement kicking, this has to be adjusted (Either that, or the kicking algo has to update the bombMask...maybe that's a better idea!)!
	    		  if (bombMask.getMaskAt(gp)==MapMask.BOMB) {
	    			  // If there is a bomb at that grid...
	    			  LocalBomb bomb=(LocalBomb) lbm.getLocalBombAt(gp);
	    			  if (bomb!=null && !pp.isRemote()) {
	    				  // Check, if it is a local one!?
		    			  if (bomb.getValue()==LocalBombManager.VALUE_ACTIVE || bomb.getValue()==LocalBombManager.VALUE_DISABLED) {
		    				  if (bomb.getValue()==LocalBombManager.VALUE_ACTIVE ) {
			    				  ClientEventQueue queue=level.getEventQueue();
			    				  Event event = new Event(Event.BOMB_DISABLED, bomb.getObjectID(), bomb.getObjectID(), bomb.getClientID());
			    				  event.setOrigin(bomb.getPosition());
			    				  event.setSourceClientID(bomb.getClientID());
			    				  queue.add(event);
		    				  }
		    				  pp.reset();
		    	    		  pp.setVisibility(Object3D.OBJ_INVISIBLE);
		    			  }
	    			  } else {
	    				  // No, it isn't. Maybe it's a remote one. This code is little bit clumsy, but it works...
	    				  Collection<ClientObject> objs=com.getClientObjects();
	    				  for (ClientObject co:objs) {
	    					  if (co instanceof BombView && (co.getBackValue()==LocalBombManager.VALUE_ACTIVE || co.getBackValue()==LocalBombManager.VALUE_DISABLED)) {
	    						  // It is a bomb and it is burning...
	    						  SimpleVector p=co.getBackPosition();
	    				    	  GridPosition gpb=bombMask.getGrid(p.x, p.z);
	    				    	  if (gp.equals(gpb)) {
	    				    		  if (co.getBackValue()==LocalBombManager.VALUE_ACTIVE && !pp.isRemote()) {
		    				    		  // We are abusing the listener here to get the corresponding local object. There is only one per bomb, we don't have to check this...:-)
		    				    		  ClientObjectCollisionListener cl=(ClientObjectCollisionListener) co.getCollisionListeners().nextElement();
		    				    		  LocalObject lob=cl.getLocalObject();
		    				    		  ClientEventQueue queue=level.getEventQueue();
		    				    		  System.out.println(lob.getObjectID()+"/"+lob.getClientID());
		    		    				  Event event = new Event(Event.BOMB_DISABLED, lob.getObjectID(), lob.getObjectID(), lob.getClientID());
		    		    				  event.setSourceClientID(lob.getClientID());
		    		    				  event.setOrigin(lob.getPosition());
		    		    				  queue.add(event);
	    				    		  }
	    		    				  pp.reset();
	    		    	    		  pp.setVisibility(Object3D.OBJ_INVISIBLE);
	    				    	  }
	    					  }
	    					  
	    				  }
	    			  }
	    		  }
	    	  }
	      }
	    }
	}
	
}
