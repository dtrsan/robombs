package robombs.game.view;

import robombs.game.util.*;

import com.threed.jpct.*;

/**
 * A particle is a client only object for doing special effects like impacts, trails...
 */
public class SmokeCloud extends Object3D {

	static final private long serialVersionUID=1L;
	
   private static Object3D PLANE=null;

   private SimpleVector vel=new SimpleVector();
   private String texture="";
   private long time=0;
   private long maxTime=-1;
   private int checkCount=0;
   private long lastMoved=0;

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
      PLANE.setTexture("smoke");
      PLANE.rotateZ((float)Math.PI/4f);
      PLANE.rotateMesh();
      PLANE.getRotationMatrix().setIdentity();
      PLANE.getMesh().compress();
      PLANE.build();
   }

   /**
    * Creates a new smoke particle
    */
   public SmokeCloud() {
      super(PLANE, true);
      setBillboarding(Object3D.BILLBOARDING_ENABLED);
      setVisibility(Object3D.OBJ_VISIBLE);
      setCulling(Object3D.CULLING_DISABLED);
      setTransparency(5);
      setLighting(Object3D.LIGHTING_NO_LIGHTS);
      enableLazyTransformations();
      reset();
      texture="smoke";
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
    * Sets the time in ms this smoke particle should live.
    * @param time long the time
    */
   public void setLifeTime(long time) {
      this.maxTime=time;
   }

   /**
    * Sets the velocity for the smoke particle.
    * @param vel SimpleVector
    */
   public void setVelocity(SimpleVector vel) {
      this.vel.set(vel);
   }

   /**
    * Resets the smoke particle.
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
    	  long dif=(long)((Ticker.getTime()-time)/1.5f);
    	  if (dif<=0) {
    		  dif=1;
    	  }
    	  int trans=Math.min(10,(int)(maxTime/dif))-1;
    	  setTransparency(trans); 
    	  for (int i=0; i<ticks; i++) {
            translate(vel);
            scale(1.02f);
         }
         checkCount+=ticks;
         lastMoved=Ticker.getTime();
         if ((maxTime!=-1 && lastMoved-time>maxTime)||trans==0) {
            reset();
            setVisibility(Object3D.OBJ_INVISIBLE);
         }
      }
   }
}
