package robombs.game.ai;

import java.util.ArrayList;
import java.util.List;

import com.threed.jpct.*;

import robombs.game.Globals;
import robombs.game.model.*;
import robombs.game.view.*;
import robombs.game.util.*;
import robombs.game.*;

public class Bot extends LocalPlayerObject {

	public final static int NO_NEW_TARGET=0;
	public final static int NEW_TARGET_CAUSE_BOMB=1;
	public final static int NEW_TARGET_CAUSE_COLLISION=2;
	public final static int NEW_TARGET_CAUSE_STUCK=3;
	
	private AIGridPosition target=null;
	private GridPosition[] path=null;
	private int pathIndex=0;
	private TargetFinder finder=new TargetFinder();
	private int waitCounter=0;
	private int newTargetLimit=0;
	private int targetScheduled=NO_NEW_TARGET;
	private long bombTimer=0;
	private GridPosition lastTarget=null;
	private GridPosition globalTarget=null;
	private MapMask decisionMask=null;
	private int gridCnt=0;
	private GridPosition lastGrid=new GridPosition(-1,-1);
	
	public void setDead() {
		super.setDead();
		target=null;
		path=null;
		pathIndex=0;
		targetScheduled=NO_NEW_TARGET;
		setSpeed(SimpleVector.ORIGIN);
	}

	public GridPosition getLastTarget() {
		return lastTarget;
	}
	
	/*
	private boolean shouldTryToEscapeFromBomb(LocalBombManager lbm, GridPosition next) {
		boolean should=lbm.getBombMask().isInBombRange(next.getX(), next.getZ(), 4);
		if (should) {
			if (Ticker.getTime()-bombTimer<LocalBombManager.LIFE_TIME) {
				return false;
			}
		}
		return should;
	}
	*/
	
	public void clearLastTarget() {
		lastTarget=null;
	}
	
	public long getBombTimer() {
		return bombTimer;
	}
	
	public boolean nextStep(BotClient client, World world, long ticks, Level level, ClientObjectManager com, LocalBombManager lbm, List<SimpleVector> playerPos, List<SimpleVector> playerPosTeam, PlayerDummy dummy) {
		
		List<SimpleVector> playerPos2=new ArrayList<SimpleVector>(playerPos);
		playerPos2.addAll(com.getPlayerPositions(true));
		playerPos2.remove(getPosition()); // A bot should not block himself!
		
		List<SimpleVector> playerPosOtherTeams=new ArrayList<SimpleVector>(playerPosTeam);
		playerPosOtherTeams.addAll(com.getPlayerPositions(true, this.getView()));
		playerPosOtherTeams.remove(getPosition());
		
		PlayerPowers pp=getPlayerPowers();
    	int sick=pp.isSick();
    	float mul=1;
    	if (sick==PlayerPowers.SLOWER) {
    		mul=0.7f;
    	}
    	if (sick==PlayerPowers.FASTER) {
    		mul=1.3f;
    	}
		
		MapMask mask=level.getMask();
		MapMask bombs=lbm.getBombMask();
		int width=mask.getWidth();
		int height=mask.getHeight();
		dummy.setObjectID(getObjectID()); // Make the dummy carry this bot's id for collision detection.
		int crashCnt=0;
		
		if (decisionMask==null || decisionMask.getWidth()!=width || decisionMask.getHeight()!=height) {
			decisionMask=new MapMask(width, height);
		}
		
		// Either the speed will be set correctly later...or not. If not, 0,0,0 is correct. because it
		// hasn't moved then...
		setSpeed(SimpleVector.ORIGIN);
		
		SimpleVector botPos=new SimpleVector(getPosition());
		SimpleVector tmp=new SimpleVector(botPos);
		GridPosition pos=mask.getGrid(botPos.x, botPos.z);
		
		if (!isDead() && this.getAnimation()!=Animations.DIE && this.getAnimation()!=Animations.DEAD) {

			if (pos.equals(lastGrid)) {
				gridCnt++;
			} else {
				lastGrid.set(pos.getX(), pos.getZ());
				gridCnt=0;
			}
			
			if (gridCnt>120) {
				targetScheduled=NEW_TARGET_CAUSE_STUCK;
				path=null;
				gridCnt=0;
			}
			
			if (Math.random()<0.002) {
				// Randomly drop a bomb...very seldom, but it happens
				client.placeBomb(this);
				path=null;
			}
			
			if (path==null || pathIndex==path.length || targetScheduled>NO_NEW_TARGET) {
				
				// Determine a new target...
				
				// Build a mask to base the decision on:
				decisionMask.clear();
				
				for (int x=0; x<width; x++) {
					for (int y=0; y<height; y++) {
						int b=bombs.getMaskAt(x, y);
						int l=mask.getMaskAt(x, y);
						int c=decisionMask.getMaskAt(x, y);
						
						if (l==MapMask.UNKNOWN && c==0) {
							decisionMask.setMaskAt(x, y, -99);
						}
						
						if (l==MapMask.CRATE && c==0) {
							decisionMask.setMaskAt(x, y, 0);
						}
						
						if (l==MapMask.FLOOR && c==0) {
							decisionMask.setMaskAt(x, y, 10);
						}
						
						if (MapMask.isItem(l) && c==0) {
							decisionMask.setMaskAt(x, y, 15);
						}
						if (b==MapMask.BOMB) {
							decisionMask.setMaskAt(x, y, -2);
							boolean left=true;
							boolean right=true;
							boolean up=true;
							boolean down=true;
							for (int p=0; p<5; p++) {
								if (p!=0) {
									int tr=mask.getMaskAt(x+p, y);
									int tl=mask.getMaskAt(x-p, y);
									int tu=mask.getMaskAt(x, y-p);
									int td=mask.getMaskAt(x, y+p);
									if (tr==MapMask.UNKNOWN) {
										right=false;
									}
									if (tl==MapMask.UNKNOWN) {
										left=false;
									}
									if (tu==MapMask.UNKNOWN) {
										up=false;
									}
									if (td==MapMask.UNKNOWN) {
										down=false;
									}
									
									if (right) {
										decisionMask.setMaskAt(x+p, y, -1);
									}
									if (left) {
										decisionMask.setMaskAt(x-p, y, -1);
									}
									if (up) {
										decisionMask.setMaskAt(x, y-p, -1);
									}
									if (down) {
										decisionMask.setMaskAt(x, y+p, -1);
									}
									
									if (!right) {
										if (tr==MapMask.FLOOR && decisionMask.getMaskAt(x+p, y)>=0) {
											decisionMask.setMaskAt(x+p, y, 5);
										}
									}
									
									if (!left) {
										if (tl==MapMask.FLOOR && decisionMask.getMaskAt(x-p, y)>=0) {
											decisionMask.setMaskAt(x-p, y, 5);
										}
									}
									
									if (!up) {
										if (tu==MapMask.FLOOR && decisionMask.getMaskAt(x, y-p)>=0) {
											decisionMask.setMaskAt(x, y-p, 5);
										}
									}
									
									if (!down) {
										if (td==MapMask.FLOOR && decisionMask.getMaskAt(x, y+p)>=0) {
											decisionMask.setMaskAt(x, y+p, 5);
										}
									}
								}
							}
						}
					}
				}
				
				//System.out.println(decisionMask.toString());
				
				AStar as=new AStar(mask, false);
				GridPosition newTarget=finder.findTarget(this, new AStar(mask, true), decisionMask, level, lbm, playerPosOtherTeams, targetScheduled);
				if (newTarget!=null) {
					globalTarget=newTarget;
					as.clear();
					as.setDecisionMask(decisionMask);
					target=as.getPathToTarget(pos, newTarget);
				}
				if (target!=null) {
					path=as.getPath(target);
					pathIndex=0;
					targetScheduled=NO_NEW_TARGET;
					newTargetLimit=2;
					/*
					for (int i=0; i<path.length; i++) {
						System.out.println("--> "+path[i]+"/"+target);
					}
					*/
				} else {
					path=null;
					newTargetLimit=10;
				}
			}
			
			if (target!=null) {
				// Normal run...
				
				SimpleVector next3D=path[pathIndex].convertTo3D();
				next3D.y=tmp.y;
				/*
				if (pos.equals(path[pathIndex])) {
					pathIndex++;
				}
				*/
				if (next3D.calcSub(tmp).length()<0.5f) {
					pathIndex++;
				}
				
				if (pathIndex<path.length) {
					GridPosition next=path[pathIndex];
					
					/*
					if (bombs.getMaskAt(pos)!=MapMask.BOMB && newTargetLimit<=0) {
						// Avoid a bomb's blow...
						for (int i=1; i<3; i++) {
							if (bombs.getMaskAt(next.getX()+i,next.getZ())==MapMask.BOMB ||
								bombs.getMaskAt(next.getX()-i,next.getZ())==MapMask.BOMB ||
								bombs.getMaskAt(next.getX(),next.getZ()+i)==MapMask.BOMB ||
								bombs.getMaskAt(next.getX(),next.getZ()-i)==MapMask.BOMB) {
									setSpeed(SimpleVector.ORIGIN);
									newTargetLimit=20;
								}
						}
					}
					*/
					
					boolean corner=false;
					// Checks if the bot move diagonally and places a bomb if needed in that case.
					if (pos.getDistanceTo(next)>1) {
						GridPosition gp1=new GridPosition(pos.getX(), next.getZ());
						GridPosition gp2=new GridPosition(next.getX(), pos.getZ());
						corner=level.getCrateManager().hasCrateAt(gp1, mask)|level.getCrateManager().hasCrateAt(gp2, mask);
					}
					
					if ((corner || level.getCrateManager().hasCrateAt(next, mask)) && bombs.getMaskAt(pos)!=MapMask.BOMB /*&& newTargetLimit<=0*/) {
						targetScheduled=NEW_TARGET_CAUSE_BOMB; // Then force a new target!
						client.placeBomb(this);
						bombTimer=Ticker.getTime();
						lastTarget=globalTarget;
					}
					
					if  (bombs.getMaskAt(next)==MapMask.BOMB && pathIndex>2) { // Don't walk into the bomb...
						targetScheduled=NEW_TARGET_CAUSE_BOMB;
						newTargetLimit=20;
					}
					
					SimpleVector newPos=next.convertTo3D();
					newPos.y=tmp.y; 
					
					if (newTargetLimit!=0) {
						newTargetLimit-=ticks;
					}
					
					float mind=10000000;
					for (SimpleVector p:playerPosOtherTeams) {
						float d=getPosition().calcSub(p).length();
						if (d<mind) {
							mind=d;
						}
					}
					if (mind<30 && Math.random()>0.3f) {
						// Enemy in range?
						client.placeBomb(this);
					}
					
					if (waitCounter<=0 && isBlockedByPlayer(newPos, com, playerPos2)) {
						// Blocked by a player? Then wait some time. Maybe the situation
						// is resolved then...
						waitCounter=(int)(15f*Math.random());
					} else {
						if (waitCounter>0) {
							// Still waiting?
							waitCounter-=ticks;
							if (waitCounter<=0) {
								// Finished waiting?
								
								if (isBlockedByPlayer(newPos, com, playerPos2)) {
									// ...but still blocked? Choose another target!
									targetScheduled=NEW_TARGET_CAUSE_COLLISION;
									newTargetLimit=10;
								} else {
									if (Math.random()<0.2f) {
										// Not blocked, no enemy in range? Drop a bomb with 20% prob.
										targetScheduled=NEW_TARGET_CAUSE_BOMB; // Then force a new target!
										client.placeBomb(this);
										newTargetLimit=20;
									}
								}
								waitCounter=0;
							}
						} else {
							
							if (newTargetLimit<=0) {
								newPos=newPos.calcSub(botPos);
								
								Matrix m=newPos.getRotationMatrix();
								m.interpolate(getRotation(), m, 0.75f);
								
								setRotation(m);
								dummy.setBackRotationMatrix(m); // Store it in the dummy...collision detection needs it
								
								SimpleVector ellipsoid=getEllipsoid();
					        	if (botPos != null && botPos.y>Globals.skyLimit) {
					        		botPos.y=-10.01f;
						
						            botPos.add(new SimpleVector(0, ellipsoid.y, 0));
						
						            SimpleVector netSpeed=new SimpleVector();
						            
						            newPos=newPos.normalize();
						            newPos.scalarMul(mul*moveSpeed*(float) ticks);
						            dummy.getTranslationMatrix().setIdentity();
						            dummy.translate(botPos);
						            
						            SimpleVector nP=new SimpleVector(newPos);
						            
						            // Disable self and some of its own bombs...
						            getView().setVisibility(false);
						            lbm.setVisibilityOfBombs(getObjectID(), Globals.botsCanPassOwnBombsForMS, false);
						            
						            newPos = dummy.checkForCollisionEllipsoid(newPos, ellipsoid, 5);
						            
						            // Enable self and some of its own bombs...
						            getView().setVisibility(true);
						            lbm.setVisibilityOfBombs(getObjectID(), Globals.botsCanPassOwnBombsForMS, true);
						            
						            if (!newPos.equals(nP)) {
						            	crashCnt++;
						            	if (crashCnt>10 || (!level.wasCollisionParticipant() && Math.random()>0.9f)) {
						            		targetScheduled=NEW_TARGET_CAUSE_COLLISION;
						            		newTargetLimit=10;
						            		setSpeed(SimpleVector.ORIGIN);
						            		newPos=SimpleVector.ORIGIN;
						            	}
						            } else {
						            	crashCnt=0;
						            }
						            
						            botPos.add(newPos);
						            
						            //netSpeed.scalarMul(1f/(float)ticks);
						            netSpeed.add(newPos);
						            netSpeed.y=0; // Ensure one level...
						            
						            setSpeed(netSpeed);
						            
						            botPos.add(new SimpleVector(0, -ellipsoid.y, 0));
						            botPos.y=-10.01f;
						            setPosition(botPos);
						        }
						}	
					} 
					}
				}
				
			}
		}
		
		return !tmp.equals(getPosition());
	}	
	
	private boolean isBlockedByPlayer(SimpleVector pos, ClientObjectManager com, List<SimpleVector> playerPos) {
		return isBlockedGeneric(pos, playerPos);
	}
	
	private boolean isBlockedGeneric(SimpleVector pos, List<SimpleVector> poss) {
		for (SimpleVector pos2:poss) {
			float delta=pos.calcSub(pos2).length();
			if (delta<10f) {
				return true;
			}
		}
		return false;
	}
	
}
