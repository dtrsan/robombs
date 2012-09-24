package robombs.game.view;

import robombs.game.util.*;

import com.threed.jpct.*;
import java.awt.*;

/**
 * A particle is a client only object for doing special effects like impacts, trails...
 */
public class Particle extends Object3D {

	static final private long serialVersionUID=1L;
	
   private final static float NO_MAX_Y=9999999999999999f;
   private static Object3D PLANE=null;
   private static final SimpleVector GRAV=new SimpleVector(0, 0.01, 0);

   private SimpleVector vel=new SimpleVector();
   private SimpleVector grav=null;
   private String texture="";
   private long time=0;
   private long maxTime=-1;
   private float maxY=NO_MAX_Y;
   private int checkCount=0;
   private long lastMoved=0;
   private boolean water=false;
   private boolean remote=false;

   /**
    * static initializer to create a PLANE-blueprint.
    */
   static {
      if (!Config.saveMemory) {
         Logger.log("jPCT isn't running in save memory mode!", Logger.WARNING);
      }
      PLANE=new Object3D(1);
      PLANE.addTriangle(new SimpleVector(-1, -1, 0), 0, 0, new SimpleVector(1, -1, 0), 1, 0,
                        new SimpleVector(1, 1, 0), 1, 1);
      PLANE.setTexture("particle");
      PLANE.rotateZ((float)Math.PI/4f);
      PLANE.rotateMesh();
      PLANE.getRotationMatrix().setIdentity();
      PLANE.getMesh().compress();
      PLANE.build();
   }

   /**
    * Creates a new particle. The texture will be "particle" and to exists in the TextureManager.
    */
   public Particle() {
      super(PLANE);
      grav=new SimpleVector(GRAV);
      rotateZ((float)Math.random()*6f);
      rotateMesh();
      getRotationMatrix().setIdentity();
      setBillboarding(Object3D.BILLBOARDING_ENABLED);
      setVisibility(Object3D.OBJ_VISIBLE);
      setCulling(Object3D.CULLING_DISABLED);
      setTransparency(10);
      setAdditionalColor(Color.white);
      setLighting(Object3D.LIGHTING_NO_LIGHTS);
      enableLazyTransformations();
      reset();
      setScale(0.8f);
      texture="particle";
   }

   /**
    * Sets the texture. This overrides setTexture in Object3D to keep an internal reference to the texture's name.
    * @param texture String the texture's name like the TextureManager knows it.
    */
   public void setTexture(String texture) {
      if (!this.texture.equals(texture)) {
         super.setTexture(texture);
         this.texture=texture;
      }
   }

   /**
    * Sets the time in ms this particle should live.
    * @param time long the time
    */
   public void setLifeTime(long time) {
      this.maxTime=time;
   }
   
   public boolean isWater() {
	   return water;
   }
   
   public void setWater(boolean isWater) {
	   water=isWater;
   }
   
   public boolean isRemote() {
	   return remote;
   }
   
   public void setColor(Color col) {
	   setAdditionalColor(col);
   }
   
   public void setAsRemote(boolean remote) {
	   this.remote=remote;
   }
   
   /**
    * Sets the y-limit, i.e. how deep the particle may fall before being discarded.
    * @param limit float the y-limit
    */
   public void setYLimit(float limit) {
      this.maxY=limit;
   }

   /**
    * Sets the velocity for the particle.
    * @param vel SimpleVector
    */
   public void setVelocity(SimpleVector vel) {
      this.vel.set(vel);
   }

   /**
    * Resets the particle.
    */
   public void reset() {
      time=Ticker.getTime();
      getTranslationMatrix().setIdentity();
      checkCount=0;
   }

   /**
    * Determines is the particle is "old". This has nothing to do with the life time. The time after
    * that a particle is considered to be old is hard-wired to 20 sec. here.
    * @return boolean is it old?
    */
   public boolean isOld() {
      return (Ticker.hasPassed(lastMoved, 20000));
   }

   /**
    * Move the particle according to it's velocity and such. If it exceed its life time or its y-limit,
    * the particle will be set to invisible.
    * @param ticks long the number of ticks passed since the last call
    */
   public void move(long ticks) {
      if (getVisibility()) {
    	  ticks<<=1; // Former version of the game had a nasty bug...move was called twice. But all particles
    	  // were designed to match that behaviour, so after correcting it, the timing was wrong. To avoid changing
    	  // all particle emitters, ticks is taken *2 here...not nice, but at least commented!
         for (int i=0; i<ticks; i++) {
            vel.add(grav);
            translate(vel);
         }
         checkCount+=ticks;
         lastMoved=Ticker.getTime();
         if ((maxTime!=-1 && lastMoved-time>maxTime)||
             (checkCount>10 && maxY!=NO_MAX_Y && getTranslation().y>=maxY)) {
            reset();
            setVisibility(Object3D.OBJ_INVISIBLE);
         }
      }
   }
}
