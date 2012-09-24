package robombs.game.view;

import robombs.game.util.*;
import robombs.clientserver.*;

import com.threed.jpct.*;
import java.awt.*;

/**
 * A simple manager for particles. I wouldn't call it a "system" for now...
 */
public class ParticleManager {

  private Particle[] parts = null;
  private World level=null;
  private long lastCleanup=0;
  private int cnt=100;

  /**
   * Creates a new particle manager with a default number of particles (100).
   * @param level World the world
   */
  public ParticleManager(World level) {
    this.level=level;
    init();
  }

  /**
   * Creates a new particle manager for a number of particles.
   * @param level World the world
   * @param max int the number of particles
   */
  public ParticleManager(World level, int max) {
     cnt=max;
     this.level=level;
     init();
  }

  /**
   * Moves all particles manmaged by this manager.
   * @param ticks long the number of ticks passed since the last call
   */
  public void move(long ticks) {
    for (int i = 0; i < cnt; i++) {
      Particle pp = parts[i];
      if (pp!=null && pp.getVisibility()) {
        pp.move(ticks);
      }
    }
    doCleanup();
  }
  
  public int getCount() {
	  return cnt;
  }
  
  public Particle[] getParticles() {
	  return parts;
  }

  /**
   * Adds a new particle to the manager. If the maximum number of active, visible particles have been reached, nothing happens.
   * This method is for adding a particle that it limited in y-direction but not in time.
   * @param pos SimpleVector the position
   * @param vel SimpleVector the velocity
   * @param limit float the y-limit
   * @param texture String the texture's name
   */
  public void addParticle(SimpleVector pos, SimpleVector vel, float limit, String texture) {
    addParticle(pos, vel, limit, -1, texture, 0.8f, true, false, false, Color.WHITE);
  }

  /**
   * Adds a new particle to the manager. If the maximum number of active, visible particles have been reached, nothing happens.
   * This method is for adding a particle that it in time but not in y-direction.
   * @param pos SimpleVector the position
   * @param vel SimpleVector the velocity
   * @param time long the life time
   * @param texture String the texture's name
   */
  public void addParticle(SimpleVector pos, SimpleVector vel, long time, String texture) {
     addParticle(pos, vel, 9999999999999999f, time, texture, 0.8f, true, false, false, Color.WHITE);
  }

  /**
   * Adds a new particle to the manager. If the maximum number of active, visible particles have been reached, nothing happens.
   * The particle will be limited in time and y-direction.
   * @param pos SimpleVector the position
   * @param vel SimpleVector the velocity
   * @param limit float the y-limit
   * @param time long the time-limit
   * @param texture String the texture's name
   */
  public void addParticle(SimpleVector pos, SimpleVector vel, float limit, long time, String texture, float size, boolean add, boolean water, boolean remote, Color col) {
    Particle p = getParticle();
    if (p!=null) {
       p.setOrigin(pos);
       p.setVelocity(vel);
       p.setLifeTime(time);
       p.setYLimit(limit);
       p.setTexture(texture);
       p.setWater(water);
       p.setAsRemote(remote);
       p.setColor(col);
       if (add) {
    	   p.setTransparencyMode(Object3D.TRANSPARENCY_MODE_ADD);
       } else {
    	   p.setTransparencyMode(Object3D.TRANSPARENCY_MODE_DEFAULT);
       }
       if (size!=1) {
    	   p.setScale(size);
       }
       p.reset();
    }
  }

  /**
   * Initializes the manager
   */
  private void init() {
     parts=new Particle[cnt];
     lastCleanup=Ticker.getTime();
  }

  /**
   * Every 30 secs, the managers checks for old particles that can be cleaned up.
   */
  private void doCleanup() {
     if (Ticker.hasPassed(lastCleanup, 30000)) {
        int counter=0;
        for (int i=cnt-1; i>=0; i--) {
           Particle pp=parts[i];
           if (pp!=null && pp.isOld()) {
              level.removeObject(pp);
              parts[i]=null;
              counter++;
           }
        }
        if (counter!=0) {
           NetLogger.log("Particle system cleanup performed: "+counter+" particles removed!");
        }
        lastCleanup=Ticker.getTime();
     }
  }

  /**
   * Gets a particle from the manager's pool. If the pool if full of particles, null will be returned.
   * @return Particle a fresh particle or null
   */
  private Particle getParticle() {
	int np=-1;  
    for (int i = 0; i < cnt; i++) {
      Particle pp = parts[i];
      if (pp!=null && !pp.getVisibility()) {
        pp.setVisibility(Object3D.OBJ_VISIBLE);
        if (pp.getScale()!=0.8f) {
        	pp.setScale(0.8f);
        }
        return pp;
      }
      if (pp==null && np==-1) {
    	  np=i;
      }
    }
    
    Particle p=null;
    
    int s=0;
    if (np!=-1) {
    	s=np;
    }
    
    for (int i=s; i<cnt; i++) {
       if (parts[i]==null) {
    	  p=new Particle();
    	  level.addObject(p);
          parts[i]=p;
          break;
       }
    }
    return p;
  }
}
