package robombs.game.view;

import robombs.game.*;
import robombs.game.model.*;
import robombs.game.sound.*;
import robombs.game.util.*;

import com.threed.jpct.*;

public class BombView extends ClientObject {

	public static final float RADIUS = 3.7f;

	static final private long serialVersionUID = 1L;

	private static Object3D bluePrint = null;
	private static Object3D bluePrintFuse = null;

	private final static float INACTIVE_SCALE = 0.4f;

	private boolean exploding = false;
	private float scale = 1f;
	private float scaleInc = -0.015f;
	private int cnt = 0;
	private float oldScale = 1f;
	private long touchedTime = 0; // A small timerout value to avoid double
	// touching of bombs

	static {
		// Static initializer for bomb blueprint
		bluePrint = Primitives.getSphere(Globals.enhancedGraphics ? 15 : 12, RADIUS);
		bluePrint.getMesh().compress();
		bluePrint.rotateX((float) Math.PI);
		bluePrint.rotateMesh();
		bluePrint.setRotationMatrix(new Matrix());

		if (!Globals.normalMapping) {
			bluePrint.setTexture("bomb1");
		} else {
			TextureInfo ti = new TextureInfo(TextureManager.getInstance().getTextureID("bomb1"));
			ti.add(TextureManager.getInstance().getTextureID("bombnormals"), TextureInfo.MODE_MODULATE);
			bluePrint.setTexture(ti);
		}
		bluePrint.calcTextureWrapSpherical();

		bluePrint.getMesh().compress();
		bluePrint.build();

		bluePrintFuse = Primitives.getCylinder(6, 0.2f, 7);
		bluePrintFuse.rotateZ(0.5f);
		bluePrintFuse.translate(-2, -4.5f, 0);
		bluePrintFuse.translateMesh();
		bluePrintFuse.setTranslationMatrix(new Matrix());
		bluePrintFuse.getMesh().compress();
		bluePrintFuse.setTexture("bomb1");
		bluePrintFuse.calcTextureWrapSpherical();
		bluePrintFuse.build();
		if (Globals.compiledObjects) {
			bluePrintFuse.compile(Globals.allDynamic);
		}

		bluePrint.translate(100000, 100000, 100000);
	}

	/**
	 * Creates a new bomb based on the blueprint.
	 */
	public BombView() {
		super(bluePrint, bluePrintFuse, false);
		createCollisionMesh();
		if (Globals.normalMapping) {
			ShaderProvider.setShader("normals", this);
		}
	}

	public BombView(ClientObject obj) {
		super(obj, bluePrintFuse, true);
		createCollisionMesh();
		if (Globals.normalMapping) {
			ShaderProvider.setShader("normals", this);
		}
	}

	private void createCollisionMesh() {
		CollisionMesh cm = new CollisionMesh(this, RADIUS);
		cm.setCollisionOptimization(Object3D.COLLISION_DETECTION_OPTIMIZED);
		setCollisionMesh(cm);
		cm.translate(0, -2, 0);
	}

	public void addCollisionListener(CollisionListener cl) {
		super.addCollisionListener(cl);
		if (colMesh != null) {
			colMesh.addCollisionListener(cl);
		}
	}

	public void enterCollisionMode(LocalPlayerObject player) {
		oldScale = getScale();
		setScale(1);
		if (colMesh != null && getVisibility() && getBackValue() != LocalBombManager.VALUE_INACTIVE) {
			collisionMode = true;
			setVisibility(false);
			SimpleVector bombPos = null;
			if (player != null) {
				bombPos = getTranslation();
				bombPos.y = player.getPosition().y;
			}
			if (player == null || bombPos.calcSub(player.getPosition()).length() >= LocalBombManager.ACTIVATION_DISTANCE * 2) {
				colMesh.setVisibility(true);
			} else {
				// If the player is located inside the bomb, it doesn't
				// block...this happens seldom,
				// but it happens. Most likely due to latency problems or
				// something. This hack avoids
				// frustration at the cost of accuracy.
				colMesh.setVisibility(false);
				SimpleVector cor = player.getPosition().calcSub(bombPos).normalize();
				player.setLocalMovementCorrection(cor);
			}
		}
	}

	public void leaveCollisionMode() {
		setScale(oldScale);
		super.leaveCollisionMode();
	}

	public void hitByExplosion(CollisionParticipant source, LocalObject obj, DecalManager decal, CollisionEvent ce) {
		Event event = new Event(Event.EXPLOSION_HIT, -99, obj.getObjectID(), obj.getClientID());
		source.getEventQueue().add(event);
	}

	public void hitByLocalPlayer(CollisionParticipant source, LocalObject obj, DecalManager decal, CollisionEvent ce) {
		ClientObject player = (ClientObject) ce.getSource();
		Object3D bomb = (Object3D) ce.getObject();

		SimpleVector dir = bomb.getTransformedCenter();
		dir = dir.calcSub(player.getTranslation());
		dir.y = 0;
		dir = dir.normalize();

		SimpleVector vd = player.getBackRotationMatrix().getZAxis();
		vd.y = 0;

		float pa = vd.x * dir.x + vd.y * dir.y + vd.z * dir.z;

		// Inaccuracies may cause NaN...fix this:
		if (pa < -1) {
			pa = -1;
		}
		if (pa > 1) {
			pa = 1;
		}

		pa = (float) Math.acos(pa);

		LocalPlayerObject tmpPlayer = (LocalPlayerObject) player.getModel();

		if (tmpPlayer == null) {
			throw new RuntimeException("Player model is null!");
		}

		long nt = Ticker.getTime() - touchedTime;

		if ((nt > 100 || nt < 0) && ((pa <= 0.7f && tmpPlayer.getPlayerPowers().canKick()) || tmpPlayer.isInvincible())) {
			Event event = new Event(Event.BOMB_TOUCHED, source.getObjectID(), obj.getObjectID(), obj.getClientID());
			event.setSourceClientID(source.getClientID());
			event.setDirection(dir);
			event.setOrigin(player.getTranslation());
			source.getEventQueue().add(event);
			touchedTime = Ticker.getTime();
		}
	}

	/**
	 * Process remote bombs.
	 * 
	 * @param ticks
	 *            time passed since the last run
	 * @param level
	 *            the current level
	 */
	public void process(long ticks, Level level) {

		SimpleVector pOld = getTransformedCenter();

		if (isModified()) {
			SimpleVector pos = getBackPosition();
			getTranslationMatrix().setIdentity();
			translate(pos);
			setModified(false);
			float scale = getScale();
			setScale(1f);
			setScale(scale);
		} else {
			SimpleVector spd = new SimpleVector(getBackSpeed());
			spd.scalarMul(ticks);
			translate(spd);
		}

		float scaleR = getScale();
		setScale(1);
		Matrix tar = getRotationMatrix().cloneMatrix();
		tar.interpolate(getRotationMatrix(), getBackRotationMatrix(), 0.4f * (float) ticks);
		setRotationMatrix(tar);
		setScale(scaleR);

		if (!pOld.equals(getTransformedCenter())) {
			SimpleVector np = getTransformedCenter();
			// The bomb has moved! We have to update the bomb mask to reflect
			// this.
			LocalBombManager lbm = level.getLocalBombManager();
			if (lbm == null) {
				throw new RuntimeException("No bomb manager assigned!");
			}
			MapMask mask = lbm.getBombMask();
			GridPosition gpn = mask.getGrid(np.x, np.z);
			GridPosition gpo = mask.getGrid(pOld.x, pOld.z);
			if (!gpn.equals(gpo)) {
				// It has crossed a grid border!
				// System.out.println("Bomb has crossed a grid's borders!");
				mask.setMaskAt(gpo, MapMask.NO_BOMB);
				mask.setMaskAt(gpn, MapMask.BOMB);
			}
		}

		int val = getBackValue();
		switch (val) {
		case LocalBombManager.VALUE_INACTIVE:
			setScale(INACTIVE_SCALE);
			setTransparency(10);
			break;
		case LocalBombManager.VALUE_ACTIVE:

			// Bomb

			scale += scaleInc;
			if (scale >= 1f) {
				scaleInc *= -1f;
				scale = 1;
			}
			if (scale <= 0.8f) {
				scaleInc *= -1f;
				scale = 0.8f;
			}
			setScale(scale);
			setTransparency(-1);
			this.setCollisionMode(Object3D.COLLISION_CHECK_OTHERS | Object3D.COLLISION_CHECK_SELF);

			// Fuse
			SimpleVector dir = child.getYAxis();
			dir.scalarMul((float) ticks / ((float) LocalBombManager.LIFE_TIME / 57.14f));
			child.translate(dir);

			ParticleManager parMan = level.getParticleManager();

			SimpleVector d = child.getTransformedCenter();
			SimpleVector sd = child.getYAxis();
			sd.x += 0.5f;
			sd.scalarMul(-0.7f);
			d.add(sd);
			SimpleVector pos = new SimpleVector(sd);
			pos.normalize();

			int sparks = Globals.sparkNumber;
			float power = Globals.sparkPower;
			int time = 400;

			if (Globals.enhancedGraphics) {
				sparks *= 2;
				power *= 1.25f;
				time = 500;
			}

			for (int i = 0; i < sparks * ticks; i++) {
				sd.set(pos);
				sd.x += 0.5f - Math.random();
				sd.y += 0.5f - Math.random();
				sd.z += 0.5f - Math.random();

				sd.scalarMul(power);

				parMan.addParticle(d, sd, time, "spark");
			}
			cnt++;
			if ((cnt & 1) == 1 || Globals.enhancedGraphics) {
				for (int i = 0; i < ticks; i++) {
					SimpleVector vel = new SimpleVector((float) Math.random() / 5f, -0.6 + ((float) Math.random() / 7f), (float) Math.random() / 5f);
					level.getSmokeCloudManager().addSmokeCloud(d, vel, 1500, "smoke", Globals.enhancedGraphics ? 4 : 2);
				}
			}

			break;
		case LocalBombManager.VALUE_EXPLODING:
			if (!exploding) {
				exploding = true;
				this.setCollisionMode(Object3D.COLLISION_CHECK_NONE);
				setVisibility(false);
				SoundManager.getInstance().play("explosion", getTranslation());
				level.getExplosionManager().addExplosion(this, level.getEventQueue(), level);
				level.getDebrisManager().createDebris(this);
			}
			break;
		case LocalBombManager.VALUE_DISABLED:
			cnt++;
			setTransparency(-1);
			if (getScale() <= INACTIVE_SCALE) {
				// Fix the scale to 1...otherwise, a remote client may still
				// have the inactive scaling, which is bad...
				setScale(1f);
			}
			if ((cnt & 8) == 8) {
				d = child.getTransformedCenter();
				sd = child.getYAxis();
				sd.x += 0.5f;
				sd.scalarMul(-0.7f);
				d.add(sd);
				for (int i = 0; i < ticks; i++) {
					SimpleVector vel = new SimpleVector((float) Math.random() / 5f, -0.6 + ((float) Math.random() / 7f), (float) Math.random() / 5f);
					level.getSmokeCloudManager().addSmokeCloud(d, vel, 1500, "smoke", 1);
				}
			}
			break;
		default:
			throw new RuntimeException("Unknown value: " + val);
		}
	}

	public void processForBotClient(long ticks, Level level) {

		SimpleVector pOld = getTransformedCenter();

		if (isModified()) {
			SimpleVector pos = getBackPosition();
			getTranslationMatrix().setIdentity();
			translate(pos);
			setModified(false);
		} else {
			SimpleVector spd = new SimpleVector(getBackSpeed());
			spd.scalarMul(ticks);
			translate(spd);
		}

		if (!pOld.equals(getTransformedCenter())) {
			SimpleVector np = getTransformedCenter();
			// The bomb has moved! We have to update the bomb mask to reflect
			// this.
			LocalBombManager lbm = level.getLocalBombManager();
			if (lbm == null) {
				throw new RuntimeException("No bomb manager assigned!");
			}
			MapMask mask = lbm.getBombMask();
			GridPosition gpn = mask.getGrid(np.x, np.z);
			GridPosition gpo = mask.getGrid(pOld.x, pOld.z);
			if (!gpn.equals(gpo)) {
				// It has crossed a grid's border!
				// System.out.println("Bomb has crossed a grid's borders!");
				mask.setMaskAt(gpo, MapMask.NO_BOMB);
				mask.setMaskAt(gpn, MapMask.BOMB);
			}
		}

		int val = getBackValue();
		switch (val) {
		case LocalBombManager.VALUE_ACTIVE:
			setScale(1);
			this.setCollisionMode(Object3D.COLLISION_CHECK_OTHERS | Object3D.COLLISION_CHECK_SELF);
			break;
		case LocalBombManager.VALUE_EXPLODING:
			if (!exploding) {
				exploding = true;
				this.setCollisionMode(Object3D.COLLISION_CHECK_NONE);
				setVisibility(false);
				level.getExplosionManager().addExplosion(this, level.getEventQueue(), level);
			}
			break;
		}
	}

	public void processLocal(long ticks, Level level, LocalObject lo) {
	}

}
