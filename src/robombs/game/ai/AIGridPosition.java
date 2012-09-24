package robombs.game.ai;

import robombs.game.model.*;

/**
 * An extended GridPosition for the A*-implementation.
 */
public class AIGridPosition extends GridPosition {

   private AIGridPosition gp;
   private float value=0;

   public AIGridPosition(GridPosition gp) {
      super(gp.getX(), gp.getZ());
   }

   public AIGridPosition(int x, int z) {
      super(x, z);
   }

   public void setParent(AIGridPosition gp) {
      this.gp=gp;
   }

   public AIGridPosition getParent() {
      return gp;
   }

   public void setValue(float val) {
      value=val;
   }

   public float getValue() {
      return value;
   }
}
