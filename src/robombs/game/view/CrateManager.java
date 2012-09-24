package robombs.game.view;


import java.util.*;
import java.awt.Color;
import com.threed.jpct.*;
import com.threed.jpct.util.ShadowHelper;

import robombs.game.model.*;
import robombs.game.util.SimpleStream;
import robombs.game.*;


public class CrateManager {
	
	private static Color[] glowColors=new Color[200];
	private List<CrateView> crates=null;
	private World world=null;
	
	// The "glow" marks crates in bomb's range.
	
	private Object3D[] glows=new Object3D[4];
	private static Object3D bluePrintGlow=null;
	private int colorIndex=0;
	
	static {
		try {
			SimpleStream ss = new SimpleStream("data/crate.3ds");
			bluePrintGlow = new ClientObject(Loader.load3DS(ss.getStream(), 0.095f)[0]);
			ss.close();
			bluePrintGlow.translate(0, -4.3f, -5.2f);
			bluePrintGlow.translateMesh();
			bluePrintGlow.getTranslationMatrix().setIdentity();
			bluePrintGlow.setTexture("glow");
			bluePrintGlow.getMesh().compress();
			bluePrintGlow.build();
		} catch(Exception e) {
			throw new RuntimeException("Unable to load glow object!");
		}
		
		for (int i=0; i<(glowColors.length>>1); i++) {
			Color col=new Color(i+28,0,0);
			glowColors[i]=col;
			glowColors[glowColors.length-1-i]=col;
		}
		
	}
	
	public CrateManager(World world) {
		crates=new ArrayList<CrateView>();
		this.world=world;
		
		for (int i=0; i<glows.length; i++) {
			Object3D glow=new Object3D(bluePrintGlow);
			glow.setTransparency(10);
			glow.setTransparencyMode(Object3D.TRANSPARENCY_MODE_ADD);
			glow.setVisibility(false);
			world.addObject(glow);
			glow.build();
			glow.setLighting(Object3D.LIGHTING_NO_LIGHTS);
			glows[i]=glow;
		}
	}
	
	public void addCrate(SimpleVector position, int id, DecalManager decMan, ClientEventQueue eventQueue, boolean botUsage) {
		CrateView e=null;
		for (CrateView ex: crates) {
			if (!ex.getVisibility()) {
				e=ex;
				break;
			}
		}
		if (e==null) {
			e=(CrateView) ClientObjectFactory.getInstance().create(Types.CRATE);
			e.setEventQueue(eventQueue);
			crates.add(e);
			e.addToWorld(world);
			e.init(id);
			e.setCollisionMode(Object3D.COLLISION_CHECK_OTHERS);
			e.setCollisionOptimization(Object3D.COLLISION_DETECTION_OPTIMIZED);
			e.addCollisionListener(new ClientObjectCollisionListener(e.getLocalObject(), decMan, true));
		}
		if (botUsage) {
			e.botUsage();
		}
		e.setDecalManager(decMan);
		e.translate(position);
	}
	
	public void processGlows(long ticks, Level level, LocalPlayerObject player) {
		PlayerPowers powers=player.getPlayerPowers();
		int width=powers.getFirePower();
		SimpleVector pos=player.getPosition();
	
		colorIndex+=ticks;
		colorIndex%=glowColors.length;
		
		for (int i=0; i<glows.length; i++) {
			glows[i].setVisibility(false);
		}
		
		MapMask mask=level.getMask();
		GridPosition gp=mask.getGrid(pos.x, pos.z);
		
		int x=gp.getX();
		int z=gp.getZ();
		
		boolean bl=false;
		boolean br=false;
		boolean bu=false;
		boolean bo=false;
		
		int wi=(int)((float)width/(float)MapMask.TILE_SIZE+0.5f)+1;
		
		for (int i=0; i<=wi; i++) {
			
			// x+
			int ml=mask.getMaskAt(x+i, z);
			
			if (!bl) {
				if (ml==MapMask.UNKNOWN) {
					bl=true;
				}
			}
			
			float delta=3f;
			if (Globals.useBombingGrid) {
				delta=Globals.bombingGridWidth;
			}
			
			if (!bl && !glows[0].getVisibility() && (ml==MapMask.CRATE || MapMask.isItem(ml))) {
				GridPosition gp2=new GridPosition(x+i,z);
				if (hasCrateAt(gp2, mask)) {
					SimpleVector posc=gp2.convertTo3D();
					if (Math.abs(pos.x-posc.x)<=width+MapMask.TILE_SIZE/2+2f && Math.abs(pos.z-posc.z)<=delta) {
						glows[0].setVisibility(true);
						glows[0].getTranslationMatrix().setIdentity();
						posc.y=0;
						glows[0].translate(posc);
						glows[0].setAdditionalColor(glowColors[colorIndex]);
					}
				} else {
					bl=true;
				}
			}
			
			//x-
			int mr=mask.getMaskAt(x-i, z);
			
			if (!br) {
				if (mr==MapMask.UNKNOWN) {
					br=true;
				}
			}
			
			if (!br && !glows[1].getVisibility() && (mr==MapMask.CRATE || MapMask.isItem(mr))) {
				GridPosition gp2=new GridPosition(x-i,z);
				if (hasCrateAt(gp2, mask)) {
					SimpleVector posc=gp2.convertTo3D();
					if (Math.abs(pos.x-posc.x)<=width+MapMask.TILE_SIZE/2+2f && Math.abs(pos.z-posc.z)<=delta) {
						glows[1].setVisibility(true);
						glows[1].getTranslationMatrix().setIdentity();
						posc.y=0;
						glows[1].translate(posc);
						glows[1].setAdditionalColor(glowColors[colorIndex]);
					}
				}else {
					br=true;
				}
			}
			
			//z+
			int mu=mask.getMaskAt(x, z+i);
			
			if (!bu) {
				if (mu==MapMask.UNKNOWN) {
					bu=true;
				}
			}
			
			if (!bu && !glows[2].getVisibility() && (mu==MapMask.CRATE || MapMask.isItem(mu))) {
				GridPosition gp2=new GridPosition(x,z+i);
				if (hasCrateAt(gp2, mask)) {
					SimpleVector posc=gp2.convertTo3D();
					if (Math.abs(pos.z-posc.z)<=width+MapMask.TILE_SIZE/2+2f && Math.abs(pos.x-posc.x)<=delta) {
						glows[2].setVisibility(true);
						glows[2].getTranslationMatrix().setIdentity();
						posc.y=0;
						glows[2].translate(posc);
						glows[2].setAdditionalColor(glowColors[colorIndex]);
					}
				}else {
					bu=true;
				}
			}
			
			//z-
			int mo=mask.getMaskAt(x, z-i);
			
			if (!bo) {
				if (mo==MapMask.UNKNOWN) {
					bo=true;
				}
			}
			
			if (!bo && !glows[3].getVisibility() && (mo==MapMask.CRATE || MapMask.isItem(mo))) {
				GridPosition gp2=new GridPosition(x,z-i);
				if (hasCrateAt(gp2, mask)) {
					SimpleVector posc=gp2.convertTo3D();
					if (Math.abs(pos.z-posc.z)<=width+MapMask.TILE_SIZE/2+2f && Math.abs(pos.x-posc.x)<=delta) {
						glows[3].setVisibility(true);
						glows[3].getTranslationMatrix().setIdentity();
						posc.y=0;
						glows[3].translate(posc);
						glows[3].setAdditionalColor(glowColors[colorIndex]);
					}
				}else {
					bo=true;
				}
			}
		}
	}
	
	public void makeCaster(ShadowHelper sh) {
		for (CrateView cv:crates) {
			sh.addCaster(cv);
			cv.setShadowHelper(sh);
		}
	}
	
	public void explode(Event event, Level level) {
		int id=event.getTargetID();
		for (CrateView cv:crates) {
			if (!cv.isInactive() && cv.getLocalObject().getObjectID()==id) {
				cv.explode(event.getOrigin());
				clearGrid(level, cv);
				break;
			}
		}
	}
	
	public void removeUponServerRequest(int id, Level level) {
		for (CrateView cv:crates) {
			if (!cv.isInactive() && cv.getLocalObject().getObjectID()==id) {
				cv.removeUponServerRequest();
				cv.removeFromWorld(world);
				clearGrid(level, cv);
				break;
			}
		}
	}
	
	public void process(long ticks, Level level) {
		if (ticks>0) {
			for (CrateView ex: crates) {
				if (ex.isExploding()) {
					ex.process(ticks, level);
					if (ex.isGone()) {
						// Crate exploded and animation is done?
						ex.removeFromWorld(world);
					}
				}
			}
		}
	}
	
	public List<SimpleVector> getCratePositions() {
		List<SimpleVector> res=new ArrayList<SimpleVector>();
		for (CrateView cr: crates) {
			if (!cr.isGone()) {
				res.add(cr.getTranslation());
			}
		}
		return res;
	}
	
	public boolean hasCrateAt(GridPosition gp, MapMask mask) {
		for (CrateView cr: crates) {
    		SimpleVector pos=cr.getTranslation();
    		GridPosition gpb=mask.getGrid(pos.x, pos.z);
    		if (gp.equals(gpb) && !cr.isGone()) {
    			return true;
    		}
    	}
    	return false;
    }
	
	private void clearGrid(Level level, CrateView crate) {
		SimpleVector pos=crate.getTransformedCenter();
		GridPosition gp=level.getMask().getGrid(pos.x, pos.z);
		if (level.getMask().getMaskAt(gp)==MapMask.CRATE) {
			// Do this only if there is no item in that crate
			level.getMask().setMaskAt(gp, MapMask.FLOOR);
		}
	}
	
}
