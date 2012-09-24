package robombs.game.ai;

import robombs.game.model.*;
import robombs.game.view.*;
import com.threed.jpct.*;
import java.util.*;

public class TargetFinder {
	
	public TargetFinder() {
	}

	public GridPosition findTarget(Bot lpo, AStar as, MapMask mask, Level le, LocalBombManager lbm, List<SimpleVector> playerPos, int mode) {
		SimpleVector pos=new SimpleVector(lpo.getPosition());
		GridPosition gp=mask.getGrid(pos.x, pos.z);
		
		int height=mask.getHeight();
		int width=mask.getWidth();
		
		int t=mask.getMaskAt(gp);
		boolean near=lbm.isTooClose(pos);
		
		int xp=gp.getX();
		int yp=gp.getZ();
		
		if ((t<0 || near) && mode!=Bot.NEW_TARGET_CAUSE_STUCK) {
			
			//System.out.println(mask);
			
			int i=1;
			GridPosition escapeTo=gp;
			do {
				// We are in danger, we have to escape to the nearest safe spot.
				for (int y=-i; y<=i; y++) {
					for (int x=-i; x<=i; x++) {
						int tt=mask.getMaskAt(x+xp, y+yp);
						if (tt>1) {
							// See, if there is a bomb in the way. If so, don't go that way...
							as.clear();
							as.setDecisionMask(mask);
							AIGridPosition target=as.getPathToTarget(gp, new GridPosition(x+xp,y+yp));
							if (target!=null) {
								GridPosition[] path=as.getPath(target);
								
								boolean ok=true;
								for (int ii=0; ii<path.length; ii++) {
									if (!path[ii].equals(gp) && (((mask.getMaskAt(path[ii])==-2 && lbm.getLocalBombAt(path[ii]).getValue()>LocalBombManager.VALUE_INACTIVE) || le.getCrateManager().hasCrateAt(path[ii], mask)))) {
										ok=false;
										break;
									}
									if (mask.getMaskAt(path[ii])<0) {
										// A blow/bomb on the way? Only cross it with a slight chance of 15%.
										if (Math.random()>0.15f) {
											ok=false;
											break;
										}
									}
								}
								
								if (ok && Math.random()>0.05f) { // Not always...
									return new GridPosition(x+xp,y+yp);
								}
							}
						}
						if (y>-i && y!=i && x==-i) {
							x=i-1;
						}
					}
				}
				i++;
			} while(i<5);
			
			return escapeTo;
		} else {
			
			if (mode==Bot.NEW_TARGET_CAUSE_STUCK) {
				// We are stuck. Only god knows, why this happens. We have to recover somehow...
				int i=1;
				do {
					// We are in danger, we have to escape to the nearest safe spot.
					for (int y=-i; y<=i; y++) {
						for (int x=-i; x<=i; x++) {
							int tt=mask.getMaskAt(x+xp, y+yp);
							if ((tt==-1 || tt==10 || t==5) && Math.random()>0.5f) {
								return new GridPosition(x+xp, y+yp);
							}
						}
					}
					i++;
				} while (i<4);
			}
			
			
			// Get the best target around a randomly choosen spot. If there is none, well...then don't move
			
			// look for a player first...
			if (Math.random()>0.3) {
				float mind=10000000;
				SimpleVector attack=null;
				for (SimpleVector p:playerPos) {
					float d=pos.calcSub(p).length();
					if (d<mind) {
						mind=d;
						attack=p;
					}
				}
				if (attack!=null) {
					return mask.getGrid(attack.x, attack.z);
					
				}
			}
			
			// look for an item second...
			if (Math.random()>0.5f) {
				List<GridPosition> items=getItems(le);
				if (items!=null && items.size()>0) {
					// Go get it.
					return items.get((int)(Math.random()*items.size()));
				}
			}
			int max=0;
			GridPosition newPos=null;
			
			xp=(int)(Math.random()*width);
			yp=(int)(Math.random()*height);
			
			for (int y=-3; y<=3; y++) {
				for (int x=-3; x<=3; x++) {
					int tt=mask.getMaskAt(x+xp, y+yp);
					if (tt>max) {
						max=tt;
						newPos=new GridPosition(x+xp,y+yp);
					}
				}
			}
			
			return newPos;
		}
	}
	
	private List<GridPosition> getItems(Level le) {
		MapMask mask=le.getMask();
		List<GridPosition> gps=null;
		int height=mask.getHeight();
		int width=mask.getWidth();
		for (int x=1; x<width; x++) {
			for (int y=1; y<height; y++) {
				int d=mask.getMaskAt(x, y);
				GridPosition gp=new GridPosition(x,y);
				if (MapMask.isItem(d) && !le.getCrateManager().hasCrateAt(gp, mask)) {
					if (gps==null) {
						gps=new ArrayList<GridPosition>();
					}
					gps.add(gp);
				}
			}
		}
		return gps;
	}
}
