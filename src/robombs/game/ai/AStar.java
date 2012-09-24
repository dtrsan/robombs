package robombs.game.ai;

import robombs.game.model.*;
import java.util.*;

/**
 * Modified A*-implementation...
 */

public class AStar {

   private final static int BLOCKED=9999;
   private final static int MAX_ITERATIONS=500;

   private MapMask mask=null;
   private MapMask dec=null;
   private Set<AIGridPosition> closed=null;
   private Set<AIGridPosition> open=null;
   private AIGridPosition result=null;
   private int itCnt=0;
   private boolean crateBlock=false;

   public AStar(MapMask mm, boolean cratesAreBlocks) {
      this.mask=mm;
      closed=new HashSet<AIGridPosition>();
      open=new HashSet<AIGridPosition>();
      crateBlock=cratesAreBlocks;
   }
   
   public void clear() {
	   closed.clear();
	   open.clear();
	   result=null;
	   itCnt=0;
	   dec=null;
   }
   
   public void setDecisionMask(MapMask dec) {
	   this.dec=dec;
   }
   
   
   public AIGridPosition getPathToTarget(GridPosition source, GridPosition target) {
      result=null;
      AIGridPosition agp=new AIGridPosition(source);
      agp.setValue(agp.getDistanceTo(target));
      getPathToTarget(agp, target);
      return result;
   }

   public GridPosition[] getPath(AIGridPosition result) {

      List<GridPosition> res=new ArrayList<GridPosition>();

      while (result.getParent()!=null) {
         res.add(result);
         result=result.getParent();
      }

      GridPosition[] gps=new GridPosition[res.size()];

      for (int i=res.size()-1; i>=0; i--) {
         gps[gps.length-1-i]=(GridPosition) res.get(i);
      }
      return gps;
   }

   private void getPathToTarget(AIGridPosition gp, GridPosition target) {
      itCnt++;

      if (itCnt>MAX_ITERATIONS) {
         // That's enough. Maybe there is no solution (yet).
         return;
      }

      AIGridPosition best=null;
      AIGridPosition opened[]=null;
      int openedCnt=0;

      int gpx=gp.getX();
      int gpz=gp.getZ();

      AIGridPosition ngp=null;

      do {

         if (opened!=null) {
            for (int i=0; i<openedCnt; i++) {
               open.remove(opened[i]);
            }
            openedCnt=0;
         }

         best=null;
         for (int x=-1; x<2; x++) {
            int xx=gpx+x;
            for (int z=-1; z<2; z++) {
               if (z!=0||x!=0) {
                  int zz=gpz+z;
                  if (!mask.isObstacle(xx, zz)) {
                     float addCost=1;
                     
                     if (!crateBlock) {
	                     if (mask.isSemiBlocked(xx, zz)) {
	                    	addCost=1.3f;
	                     }
                     } else {
                    	 if (mask.isBlocked(xx, zz)) {
 	                    	addCost=BLOCKED;
 	                     }
                     }
                     
                     if (z!=0&&x!=0 && addCost!=BLOCKED) {
                        addCost=3f;
                        int cc=mask.doCornerCheck(gp, x, z);
                        if (cc==MapMask.CORNER_SINGLE) {
                           addCost=5000;
                        } else {
                           if (cc==MapMask.CORNER_DOUBLE) {
                              addCost=BLOCKED;
                           }
                        }
                     }

                     if (dec!=null && addCost!=BLOCKED) {
                    	 int di=dec.getMaskAt(xx, zz);
                    	 if (di==-1) {
                    		 // Bomb's blow ahead...cross this only if needed!
                    		 addCost=500f;
                    	 }
                    	 if (di==-2) {
                    		 // Bomb ahead...don't go there
                    		 addCost=BLOCKED;
                    	 }
                     }
                     
                     if (addCost!=BLOCKED) {
                        if (ngp==null) {
                           ngp=new AIGridPosition(xx, zz);
                        } else {
                           // Recycling of unused instances...
                           ngp.set(xx,zz);
                        }
                        if (!open.contains(ngp)&&!closed.contains(ngp)) {
                           if (opened==null) {
                              opened=new AIGridPosition[8];
                           }
                           open.add(ngp);
                           opened[openedCnt]=ngp;
                           openedCnt++;
                           ngp.setParent(gp);
                           ngp.setValue(target.getDistanceTo(ngp)+addCost);
                           if (best==null) {
                              best=ngp;
                           } else {
                              if (ngp.getValue()<best.getValue()) {
                                 best=ngp;
                              }
                           }
                           ngp=null;
                        }
                     }
                  }
               }
            }
         }
         if (best!=null) {

            if (best.equals(target)) {
               result=best;
               return;
            }

            getPathToTarget(best, target);
         }
      } while (best!=null && result==null && itCnt<MAX_ITERATIONS);
      
      closed.add(gp);
      open.remove(gp);
   }
}
