package robombs.game.view;

import com.threed.jpct.*;

/**
 * A decal is what remains if a bullets hits something. Shamelessly ripped from jPCT's car example.<br>
 * In this example "game", only local bullet will leave decals. Remote bullets won't. And because bullets
 * are view objects only, they are not based on data from a local object and extend Object3D directly.
 * Not used in this game ATM!!!
 */
public class Decal extends Object3D {

   static final private long serialVersionUID=1L;
	
   private static final int TRANSPARENCY=10;

   private static long counter=0;
   private static Object3D DECAL=null;
   private long id=0;
   private Object3D parent=null;
   
   static {
      DECAL=Primitives.getPlane(1,3);
      DECAL.invert();
      DECAL.setTexture("decal");
      DECAL.getMesh().compress();
      DECAL.setTransparency(TRANSPARENCY);
      DECAL.build();
   }

   /**
    * A decal is a plane with a partially transparent texture.
    */
   public Decal() {
      super(DECAL, true);
      setVisibility(Object3D.OBJ_INVISIBLE);
   }

   /**
    * Places the decal
    * @param pos the position of the decal
    */
   public void place(SimpleVector pos) {
      setVisibility(true);
      setOrigin(pos);
      id=counter;
      counter++;
   }
   
   public void setParent(Object3D obj) {
	   this.parent=obj;
   }
   
   public Object3D getParent() {
	   return parent;
   }

   /**
    * Returns the id. The id is used to determine the age of a decal.
    * @return the id
    */
   public long getDecalID() {
      return id;
   }
}
