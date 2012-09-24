package robombs.game.view;

import java.awt.Color;
import java.util.*;
import robombs.game.model.*;
import robombs.clientserver.*;
import robombs.game.*;

import com.threed.jpct.*;
import com.threed.jpct.util.*;

/**
 * An Object3D that represents the actual level. This example "game" only has
 * one level. If you want more, you'll have to implement a kind of level
 * management yourself.
 */
public class Level {

	static final private long serialVersionUID = 1L;

	private final static int WALL_OCTREE_SIZE = 100;
	private final static int FLOOR_OCTREE_SIZE = 100;
	private final static int TOP_OCTREE_SIZE = 100;

	private ParticleManager parMan = null;
	private SmokeCloudManager smokeMan = null;
	private ExplosionManager exploMan = null;
	private CrateManager crateMan = null;
	private ItemManager itemMan=null;
	private DebrisManager debrisMan=null;

	private LocalBombManager lbm=null;

	private LevelPart floor = null;
	private LevelPart top = null;
	private LevelPart walls = null;
	private LevelPart shadows = null;
	private MapMask mask = null;
	private int crateCnt=0;
	private ClientEventQueue eventQueue=null;

	public Level(String file, String set, World world, ClientEventQueue eventQueue, DecalManager decMan, boolean botUsage) throws Exception {

		this.eventQueue=eventQueue;
		exploMan = new ExplosionManager(world);
		crateMan = new CrateManager(world);
		itemMan=new ItemManager(world);

		List<?> parts = LevelLoader.loadMap(file, set);
		walls = (LevelPart) parts.get(0);
		top = (LevelPart) parts.get(1);
		floor = (LevelPart) parts.get(2);
		mask = (MapMask) parts.get(3);
		shadows=(LevelPart) parts.get(6);

		shadows.enableLazyTransformations();
		shadows.getMesh().compress();
		shadows.build();

		walls.enableLazyTransformations();
		walls.getMesh().compress();
		walls.build();
		OcTree ocw = new OcTree(walls.getMesh(), WALL_OCTREE_SIZE, OcTree.MODE_OPTIMIZED);
		ocw.setCollisionUse(OcTree.COLLISION_USE);
		walls.setOcTree(ocw);
		walls.setCollisionOptimization(Object3D.COLLISION_DETECTION_OPTIMIZED);
		walls.setCollisionMode(Object3D.COLLISION_CHECK_OTHERS);

		if (!botUsage) {
			parMan = new ParticleManager(world, Globals.enhancedGraphics?4000:2000);
			smokeMan=new SmokeCloudManager(world, Globals.enhancedGraphics?1000:400);
			debrisMan=new DebrisManager(world, Globals.enhancedGraphics?80:50);
			top.enableLazyTransformations();
			floor.enableLazyTransformations();

			top.getMesh().compress();
			floor.getMesh().compress();

			top.build();
			floor.build();
			top.setLighting(Object3D.LIGHTING_NO_LIGHTS);
			top.setAdditionalColor(new java.awt.Color(50,50,50));

			OcTree ocf = new OcTree(floor.getMesh(), FLOOR_OCTREE_SIZE,
					OcTree.MODE_OPTIMIZED);
			OcTree oct = new OcTree(top.getMesh(), TOP_OCTREE_SIZE,
					OcTree.MODE_OPTIMIZED);

			OcTree ocs = new OcTree(shadows.getMesh(), FLOOR_OCTREE_SIZE,
					OcTree.MODE_OPTIMIZED);

			ocf.setCollisionUse(OcTree.COLLISION_USE);
			oct.setCollisionUse(OcTree.COLLISION_USE);

			floor.setOcTree(ocf);
			top.setOcTree(oct);
			shadows.setOcTree(ocs);
			
		} else {
			// Levels for bots don't need this.
			top=null;
			floor=null;
			shadows=null;
			parMan = new ParticleManager(world, 500); // Some particles are needed for the water...
			smokeMan=new SmokeCloudManager(world, 0);
			debrisMan=new DebrisManager(world, 0);
		}

		createCrates(decMan, botUsage);
	}

	public void setLocalBombManager(LocalBombManager lbm) {
		this.lbm=lbm;
	}

	public LocalBombManager getLocalBombManager() {
		return lbm;
	}

	public void makeReceiver(ShadowHelper sh) {
		sh.addReceiver(floor);
		sh.addReceiver(walls);
		if (Globals.renderWallShadows) {
			if (Globals.useShadowMesh) {
				sh.addCaster(shadows);
			} else {
				sh.addCaster(walls);
			}
		}
	}

	public ClientEventQueue getEventQueue() {
		return eventQueue;
	}

	public void optimizeForBotUsage() {

	}
	
	public ReflectionHelper enableReflections(FrameBuffer fb) {
		ReflectionHelper refHelper=new ReflectionHelper(fb, floor, 1024, TextureInfo.MODE_MODULATE);
		refHelper.setClippingPlaneOffset(1);
		refHelper.setClearColor(Color.WHITE);
		return refHelper;
	}

	public void addToWorld(World world) {
		world.addObject(walls);
		if (floor!=null) {
			world.addObject(floor);
		}
		if (top!=null) {
			world.addObject(top);
		}
		if (shadows!=null) {
			world.addObject(shadows);
		}
	}

	public void shadowPolygonsVisible(boolean vis) {
		if (shadows!=null) {
			if (vis) {
				shadows.setVisibility(true);
			} else {
				shadows.setVisibility(false);
			}
		}
	}

	public void addCollisionListener(CollisionListener colLis) {
		walls.addCollisionListener(colLis);
	}

	public int getWidth() {
		return mask.getWidth();
	}

	public int getHeight() {
		return mask.getHeight();
	}

	public SimpleVector getCenter() {
		return floor.getTransformedCenter();
	}

	public Camera createAllViewingCamera(SimpleVector position, FrameBuffer buffer) {
		Camera cam=new Camera();
		try {
		cam.setFOVLimits(0, 100);
		cam.setFOV(1);
		cam.setYFOV(1);

		SimpleVector midPoint=floor.getTransformedCenter();

		//midPoint.x=(position.x+midPoint.x*(getWidth()-6))/(getWidth()-5);
		//midPoint.z=(position.z+midPoint.z*(getHeight()-6))/(getHeight()-5);

		midPoint.x=(position.x+midPoint.x*10f)/11f;
		midPoint.z=(position.z+midPoint.z*10f)/11f;

		cam.setPosition(position);
		cam.lookAt(midPoint);

		float[] bb=walls.getMesh().getBoundingBox();

		// minX, maxX, minY, maxY, minZ, maxZ
		SimpleVector c1=new SimpleVector(bb[1],bb[3],bb[4]);
		SimpleVector c2=new SimpleVector(bb[1],bb[3],bb[5]);
		SimpleVector c3=new SimpleVector(bb[0],bb[3],bb[4]);
		SimpleVector c4=new SimpleVector(bb[0],bb[3],bb[5]);

		cam.copyBackToFront();

		SimpleVector c1s=Interact2D.project3D2D(cam, buffer, c1);
		SimpleVector c2s=Interact2D.project3D2D(cam, buffer, c2);
		SimpleVector c3s=Interact2D.project3D2D(cam, buffer, c3);
		SimpleVector c4s=Interact2D.project3D2D(cam, buffer, c4);

		//System.out.println(c1s.x+"/"+c1s.y+ " - "+ c2s.x+"/"+c2s.y+ " - "+c3s.x+"/"+c3s.y+ " - "+c4s.x+"/"+c4s.y);

		float ymin=999999;
		float ymax=-999999;
		float xmin=999999;
		float xmax=-999999;

		if (c1s!=null) {
			if (c1s.x<xmin) xmin=c1s.x;
			if (c1s.x>xmax) xmax=c1s.x;
			if (c1s.y<ymin) ymin=c1s.y;
			if (c1s.y>ymax) ymax=c1s.y;
		}

		if (c2s!=null) {
			if (c2s.x<xmin) xmin=c2s.x;
			if (c2s.x>xmax) xmax=c2s.x;
			if (c2s.y<ymin) ymin=c2s.y;
			if (c2s.y>ymax) ymax=c2s.y;
		}

		if (c3s!=null) {
			if (c3s.x<xmin) xmin=c3s.x;
			if (c3s.x>xmax) xmax=c3s.x;
			if (c3s.y<ymin) ymin=c3s.y;
			if (c3s.y>ymax) ymax=c3s.y;
		}

		if (c4s!=null) {
			if (c4s.x<xmin) xmin=c4s.x;
			if (c4s.x>xmax) xmax=c4s.x;
			if (c4s.y<ymin) ymin=c4s.y;
			if (c4s.y>ymax) ymax=c4s.y;
		}

		xmin=(buffer.getOutputWidth()/2)-xmin;
		xmax=xmax-(buffer.getOutputWidth()/2);

		ymin=(buffer.getOutputHeight()/2)-ymin;
		ymax=ymax-(buffer.getOutputHeight()/2);

		float xd=xmin;
		if (xmax>xd) {
			xd=xmax;
		}

		float yd=ymin;
		if (ymax>yd) {
			yd=ymax;
		}

		float xfov=xd/(buffer.getOutputWidth()/2);
		float yfov=yd/(buffer.getOutputHeight()/2);

		cam.setFOV(xfov);
		cam.setYFOV(yfov);

		} catch(Exception e) {
			e.printStackTrace();
		}

		return cam;
	}

	/**
	 * Returns a simple checksum to ensure (well, not really...) that all
	 * clients are using the same level. This has to be improved for "real"
	 * games to prevent cheating. For this example, it's fine the way it is.
	 *
	 * @return int the checksum
	 */
	public int getChecksum() {
		return walls.getMesh().getTriangleCount()
				* walls.getMesh().getUniqueVertexCount()
				+ walls.getMesh().getVertexCount();
	}

	/**
	 * Returns the particle emitter if enabled. If not, it returns null.
	 *
	 * @return ParticleManager the emitter or null
	 */
	public ParticleManager getParticleManager() {
		return parMan;
	}

	public SmokeCloudManager getSmokeCloudManager() {
		return smokeMan;
	}

	public ExplosionManager getExplosionManager() {
		return exploMan;
	}

	public CrateManager getCrateManager() {
		return crateMan;
	}

	public ItemManager getItemManager() {
		return itemMan;
	}

	public DebrisManager getDebrisManager() {
		return debrisMan;
	}

	public SimpleVector[] getSpawnPoints() {
		SimpleVector[] sorty=new SimpleVector[9];
		List<SimpleVector> points=new ArrayList<SimpleVector>();
		int x = mask.getWidth();
		int y = mask.getHeight();
		for (int i = 0; i < x; i++) {
			for (int p = 0; p < y; p++) {
				int c=mask.getMaskAt(i, p);
				if (c>=10049 && c<=10057) {
					SimpleVector np=new GridPosition(i,p).convertTo3D();
					np.y=-10;
					sorty[c-10049]=np;
					mask.setMaskAt(i,p,MapMask.FLOOR);
				}
			}
		}
		
		for (SimpleVector s:sorty) {
			if (s!=null) {
				points.add(s);
			}
		}
		
		return points.toArray(new SimpleVector[points.size()]);
	}

	public void populate(LocalBombManager bombMan, SimpleClient client, World world, ClientEventQueue eventQueue) {
		int x = mask.getWidth();
		int y = mask.getHeight();
		for (int i = 0; i < x; i++) {
			for (int p = 0; p < y; p++) {
				if (!mask.isObstacle(i, p) && !mask.isBlocked(i,p)) {
					LocalPlayerObject lop = new LocalPlayerObject();
					SimpleVector pos = new GridPosition(i, p).convertTo3D();
					pos.y = -10f;
					lop.setPosition(pos);
					bombMan.addBomb(client, lop, world, eventQueue);
				}
			}
		}
	}

	public void populateSparse(LocalBombManager bombMan, SimpleClient client, World world, ClientEventQueue eventQueue) {
		int i = (int)((float)mask.getWidth()*Math.random());
		int p = (int)((float)mask.getHeight()*Math.random());
		if (!mask.isObstacle(i, p) && !mask.isBlocked(i,p)) {
			LocalPlayerObject lop = new LocalPlayerObject();
			SimpleVector pos = new GridPosition(i, p).convertTo3D();
			pos.y = -10f;
			lop.setPosition(pos);
			bombMan.addBomb(client, lop, world, eventQueue);
		}
	}

	public MapMask getMask() {
		return mask;
	}

	public boolean createItem(SimpleVector position,LocalObject lob, ShadowHelper sh) {
		GridPosition pos=mask.getGrid(position.x, position.z);
		int field=mask.getMaskAt(pos);
		if (field==MapMask.BOMB_ITEM) {
			itemMan.addItem(position, lob.getObjectID(), Types.BOMB_ITEM, sh,eventQueue);
			return true;
		}
		if (field==MapMask.FIREPOWER_ITEM) {
			itemMan.addItem(position, lob.getObjectID(), Types.FIREPOWER_ITEM, sh,eventQueue);
			return true;
		}
		if (field==MapMask.KICK_ITEM) {
			itemMan.addItem(position, lob.getObjectID(), Types.KICK_ITEM, sh,eventQueue);
			return true;
		}
		if (field==MapMask.DISEASE_ITEM) {
			itemMan.addItem(position, lob.getObjectID(), Types.DISEASE_ITEM, sh,eventQueue);
			return true;
		}
		return false;
	}

	public boolean wasCollisionParticipant() {
		return walls.wasTargetOfLastCollision();
	}

	private void createCrates(DecalManager decMan, boolean botUsage) {
		int x = mask.getWidth();
		int y = mask.getHeight();
		for (int i = 0; i < x; i++) {
			for (int p = 0; p < y; p++) {
				int field=mask.getMaskAt(i, p);
				if (field==MapMask.CRATE || MapMask.isItem(field)) {
					SimpleVector pos = new GridPosition(i, p).convertTo3D();
					crateMan.addCrate(pos, crateCnt, decMan, eventQueue, botUsage);
					crateCnt++;
				}
			}
		}
	}

}
