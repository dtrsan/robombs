package robombs.game.view;

import robombs.game.Event;
import robombs.game.Globals;
import robombs.game.model.*;
import robombs.game.util.*;

import com.threed.jpct.*;

/**
 * A mesh representing a player. Instances are usually created by the
 * ClientObjectFactory.
 */
class EnemyView extends AnimatedObject {

	static final private long serialVersionUID = 1L;

	private static Object3D bluePrint = null;
	private static Object3D sphereBluePrint = null;

	private Object3D shield = null;

	static {
		// static initializer that creates a mesh blueprint and loads the
		// textures.
		try {
			SimpleStream ss = new SimpleStream("data/weapon.jpg");
			TextureManager.getInstance().addTexture("weapon", new Texture(ss.getStream()));
			ss.close();

			ss = new SimpleStream("data/snork.md2");
			bluePrint = new ClientObject(Loader.loadMD2(ss.getStream(), 0.22f)); // 0.32f
			bluePrint.translate(0, 4.5f, 0);
			bluePrint.translateMesh();
			bluePrint.getTranslationMatrix().setIdentity();
			bluePrint.translate(100000, 100000, 100000);

			sphereBluePrint = Primitives.getSphere(9, 6);
			sphereBluePrint.build();
			sphereBluePrint.setTexture("shield");

			sphereBluePrint.calcTextureWrapSpherical();

			ss.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Create a new enemy based on the blueprint.
	 */
	EnemyView() {
		super(bluePrint);
		createCollisionMesh();
	}

	/**
	 * Create a new enemy based on a base object and weapon.
	 * 
	 * @param obj
	 *            the base object
	 * @param weapon
	 *            the base weapon
	 */
	EnemyView(Object3D obj, Object3D weapon) {
		super(obj, weapon);
		createCollisionMesh();
		createShieldMesh();
	}

	private void createCollisionMesh() {
		CollisionMesh cm = new CollisionMesh(this, 4);
		cm.setCollisionOptimization(Object3D.COLLISION_DETECTION_OPTIMIZED);
		setCollisionMesh(cm);
		createShieldMesh();
	}

	private void createShieldMesh() {
		shield = new Object3D(sphereBluePrint);
		shield.setVisibility(false);
		shield.setMesh(sphereBluePrint.getMesh());
		shield.build();
		shield.translate(0, 4.5f, 0);
		shield.setTransparency(7);
		shield.setTransparencyMode(Object3D.TRANSPARENCY_MODE_ADD);

		if (Globals.compiledObjects) {
			shield.compile();
		}

		addChildToSuper(shield);
	}

	public void processSpecial(Object obj) {
		LocalPlayerObject lob = (LocalPlayerObject) obj;
		if (lob.shouldbeTransparent()) {
			setTransparency(11);
		} else {
			setTransparency(-1);
		}
	}

	public void setToLocalObject(LocalObject lo) {
		super.setToLocalObject(lo);
		if (lo.isInvincible() && !shield.getVisibility()) {
			shield.setVisibility(true);
		} else {
			if (!lo.isInvincible()) {
				shield.setVisibility(false);
			}
		}
	}

	public void addToWorld(World world) {
		super.addToWorld(world);
		world.addObject(shield);
	}

	public void removeFromWorld(World world) {
		super.removeFromWorld(world);
		world.removeObject(shield);
	}

	public void hitByExplosion(CollisionParticipant source, LocalObject obj, DecalManager decal, CollisionEvent ce) {
		if (shield.getVisibility()) {
			// If the shield is visible, we are invincible...
			return;
		}
		Event event = new Event(Event.EXPLOSION_HIT, -99, obj.getObjectID(), obj.getClientID());
		source.getEventQueue().add(event);
	}

	public void process(long ticks, Level level) {
		super.process(ticks, level);
		if (shield.getVisibility()) {
			shield.rotateY((float) ticks * 0.3f);
			// shield.setScale(1+(float)Math.sin(shield.getZAxis().x)/6f);
		}
	}

}
