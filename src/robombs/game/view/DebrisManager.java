package robombs.game.view;

import com.threed.jpct.*;

public class DebrisManager {

   private int maxDebris=100;

   private Debris[] debris=null;

   public DebrisManager(World world) {
      this(world, 20);
   }

   public DebrisManager(World level, int maxDebris) {
      this.maxDebris=maxDebris;
      debris=new Debris[maxDebris];

      for (int i=0; i<maxDebris; i++) {
    	 debris[i]=new Debris();
         level.addObject(debris[i]);
      }
   }

   /**
    * Sets all decals to invisible.
    */
   public void reset() {
      for (int i=0; i<maxDebris; i++) {
         debris[i].setVisibility(Object3D.OBJ_INVISIBLE);
      }
   }
   
   public void createDebris(ClientObject bomb) {
	   Debris debris=getFreeDebris();
       if (debris!=null) {
    	  debris.setTransparency(Debris.TRANSPARENCY);
    	  SimpleVector pos=new SimpleVector(bomb.getBackPosition());
    	  pos.y=-0.1f;
    	  debris.place(pos);
       }
   }

   public void process(long ticks) {
	   for (int i=0; i<maxDebris; i++) {
		   if (debris[i].getVisibility()) {
			   debris[i].process(ticks);
		   }
	   }
   }
   
   private Debris getFreeDebris() {
      long min=Long.MAX_VALUE;
      Debris deb=null;
      for (int i=0; i<maxDebris; i++) {
         if (!debris[i].getVisibility()) {
            return debris[i];
         }

         if (debris[i].getDebrisID()<min) {
            min=debris[i].getDebrisID();
            deb=debris[i];
         }
      }
      return deb;
   }
}
