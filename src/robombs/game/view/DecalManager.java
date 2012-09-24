package robombs.game.view;

import com.threed.jpct.*;

/**
 * A simple manager class for managing decals. Decals are easy to handle because they exist on the client only.
 * Not used in this game ATM!!!
 */
public class DecalManager {

   private int maxDecals=100;

   private Decal[] decals=null;

   /**
    * Create a new decal manager with a default number of decals.
    * @param world the world
    */
   public DecalManager(World world) {
      this(world, 1); // Set to one, because it's unused ATM
   }

   /**
    * Create a new decal manager with a maximum number of decals.
    * @param level the world.
    * @param maxDecals the max. number of decals
    */
   public DecalManager(World level, int maxDecals) {
      this.maxDecals=maxDecals;
      decals=new Decal[maxDecals];

      for (int i=0; i<maxDecals; i++) {
         decals[i]=new Decal();
         level.addObject(decals[i]);
      }
   }

   /**
    * Sets all decals to invisible.
    */
   public void reset() {
      for (int i=0; i<maxDecals; i++) {
         decals[i].setVisibility(Object3D.OBJ_INVISIBLE);
      }
   }
   
   public void createDecal(Object3D bullet, Object3D target, CollisionEvent e, float scale) {
	   if (target.getVisibility()) {
		   target.disableCollisionListeners();
	       bullet.disableCollisionListeners();
	       SimpleVector za = bullet.getZAxis();
	       SimpleVector tc = bullet.getTransformedCenter();
	       tc = tc.calcSub(za); // a small offset...
	       float d = target.calcMinDistance(tc, za, 50);
	       SimpleVector zas = new SimpleVector(za);
	       zas.scalarMul( -0.15f);
	       if (d != Object3D.COLLISION_NONE) {
	           za.scalarMul(d);
	           za.add(zas);
	           tc.add(za);
	           int[] ids = e.getPolygonIDs();
	           if (ids != null && ids.length > 0) {
	               int id = ids[0];
	               SimpleVector n = e.getObject().getPolygonManager().getTransformedNormal(id);
	               Decal decal=createDecal(tc, n, scale);
	               if (target instanceof CrateView) {
	            	   if (decal!=null) {
	            		   decal.setParent(target);
	            	   }
	               } 
	           }
	       }
	       target.enableCollisionListeners();
	       bullet.enableCollisionListeners();
	   }
   }

   /**
    * Creates a new decal at the given position with the given orientation.
    * @param pos the position
    * @param normal the orientation. The decal will be placed perpendicular to the normal
    */
   public Decal createDecal(SimpleVector pos, SimpleVector normal) {
      return createDecal(pos, normal, 1f);
   }
   
   public Decal createDecal(SimpleVector pos, SimpleVector normal, float scale) {
      Decal decal=getFreeDecal();
      if (decal!=null) {
         decal.place(pos);
         decal.setScale(1f);
         decal.setRotationMatrix(normal.getRotationMatrix());
         decal.setScale(scale);
      }
      return decal;
   }
   
   public void parentRemoved(Object3D parent) {
	   for (int i=0; i<maxDecals; i++) {
		   Decal decal=decals[i];
		   if (decal.getParent()==parent) {
			   decal.setParent(null);
			   decal.setVisibility(false);
		   }
	   }
   }

   /**
    * Gets a decal from the managers internal decal array
    * @return Decal the decal
    */
   private Decal getFreeDecal() {
      long min=Long.MAX_VALUE;
      Decal decal=null;
      for (int i=0; i<maxDecals; i++) {
         if (!decals[i].getVisibility()) {
        	decals[i].setParent(null);
            return decals[i];
         }

         if (decals[i].getDecalID()<min) {
            min=decals[i].getDecalID();
            decal=decals[i];
         }
      }
      decal.setParent(null);
      return decal;
   }
   
   
   
   
}
