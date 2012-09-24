package robombs.game.view;

import com.threed.jpct.*;
import robombs.game.util.*;

public class Debris extends Object3D {

   static final private long serialVersionUID=1L;
	
   public static final int TRANSPARENCY=13;

   private static long counter=0;
   private static Object3D DEBRIS=null;
   private long id=0;
   private long start=0;
   
   static {
	   DEBRIS=Primitives.getPlane(1,20);
	   DEBRIS.invert();
	   DEBRIS.setTexture("debris");
	   DEBRIS.getMesh().compress();
	   DEBRIS.setTransparency(TRANSPARENCY);
	   DEBRIS.rotateX((float)Math.PI/-2f);
	   DEBRIS.build();
   }

   public Debris() {
      super(DEBRIS, true);
      setLighting(Object3D.LIGHTING_NO_LIGHTS);
      setVisibility(Object3D.OBJ_INVISIBLE);
   }

   public void place(SimpleVector pos) {
      setVisibility(true);
      setOrigin(pos);
      id=counter;
      counter++;
      start=Ticker.getTime();
      rotateY(2f*(float)Math.random());
   }
   
   public void process(long ticks) {
	   if (getVisibility()) {
		   if (Ticker.hasPassed(start, 7000)) {
			   int newTrans=Math.max(0, getTransparency()-(int)ticks);
			   setTransparency(newTrans);
			   if (newTrans==0) {
				   setVisibility(false);
			   }
		   }
	   }
   }
   
   public long getDebrisID() {
      return id;
   }
}
