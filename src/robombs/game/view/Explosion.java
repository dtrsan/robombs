package robombs.game.view;

import com.threed.jpct.*;

import robombs.game.ClientEventQueue;
import robombs.game.Globals;
import robombs.game.model.*;
import robombs.game.util.*;

public class Explosion extends Object3D implements CollisionParticipant {

	static final private long serialVersionUID = 1L;

	private static Object3D blueprint = null;
	private static final int MAX_PER_SIDE = Globals.enhancedGraphics ? 8 : 4;

	static {
		blueprint = Primitives.getPlane(1, 10);
		blueprint.translate(100000, 100000, 100000);
		blueprint.setTexture("explosion");
		blueprint.setBillboarding(true);
		blueprint.setTransparency(11);
		blueprint.setTransparencyMode(Object3D.TRANSPARENCY_MODE_ADD);
		blueprint.setAdditionalColor(java.awt.Color.WHITE);
		blueprint.getMesh().compress();
		blueprint.build();
	}

	private Object3D[] blow = new Object3D[MAX_PER_SIDE * 4];
	private int[] index = new int[MAX_PER_SIDE * 4];
	private int mainIndex = 0;
	private long cTicks = 0;
	private int maxLength = 20;
	private long maxTicks = 1000;
	private long start = 0;
	private int blows = 3;

	private float maxXP = 1000;
	private float maxXM = 1000;
	private float maxZP = 1000;
	private float maxZM = 1000;

	private int clientID = -99999999;
	private int objectID = -99;

	private boolean isLocal = false;

	private transient ClientEventQueue eventQueue = null;

	public Explosion(boolean local) {
		super(blueprint, true);
		setScale(3);
		setTransparency(13);
		for (int i = 0; i < blow.length; i++) {
			blow[i] = new Object3D(blueprint);
		}
		isLocal = local;
	}

	public void addToWorld(World world) {
		world.addObject(this);
		for (int i = 0; i < blow.length; i++) {
			world.addObject(blow[i]);
		}
		setCollisionMode(Object3D.COLLISION_CHECK_SELF);
	}

	public void remove(World world) {
		world.removeObject(this);
		for (int i = 0; i < blow.length; i++) {
			world.removeObject(blow[i]);
		}
		setCollisionMode(Object3D.COLLISION_CHECK_NONE);
	}

	public void enable(SimpleVector startPos, ClientObject bomb) {
		getTranslationMatrix().setIdentity();
		translate(startPos);
		setTexture(this, 0);
		mainIndex = 0;
		setVisibility(true);
		maxLength = bomb.getBackAnimationSpeed();
		blows = Math.min((int) (maxLength / 7.5f + 0.5f), MAX_PER_SIDE);
		maxTicks = bomb.getBackAnimationSpeed() * LocalBombManager.DURATION_MUL;
		for (int i = 0; i < blows << 2; i++) {
			blow[i].getTranslationMatrix().setIdentity();
			blow[i].translate(startPos);
			setTexture(blow[i], 0);
			index[i] = 0;
			blow[i].setVisibility(true);
		}
		cTicks = 0;
		start = Ticker.getTime();
	}

	public void disable() {
		this.setVisibility(false);
		for (int i = 0; i < blow.length; i++) {
			blow[i].setVisibility(false);
		}
	}

	public boolean isLocal() {
		return isLocal;
	}

	public void setLocal(boolean local) {
		isLocal = local;
	}

	public void hits(CollisionParticipant target, LocalObject obj, DecalManager decal, CollisionEvent ce) {
		if (isLocal()) {
			target.hitByExplosion(this, obj, decal, ce);
		}
	}

	public void hitByExplosion(CollisionParticipant source, LocalObject obj, DecalManager decal, CollisionEvent ce) {
		// do nothing
	}

	public void hitByLocalPlayer(CollisionParticipant source, LocalObject obj, DecalManager decal, CollisionEvent ce) {
		// do nothing
	}

	public void setClientID(int id) {
		clientID = id;
	}

	public int getClientID() {
		return clientID;
	}

	public void setObjectID(int id) {
		objectID = id;
	}

	public int getObjectID() {
		return objectID;
	}

	public void setEventQueue(ClientEventQueue queue) {
		this.eventQueue = queue;
	}

	public ClientEventQueue getEventQueue() {
		return eventQueue;
	}

	public boolean process(long ticks) {
		cTicks = Ticker.getTime() - start;
		if (cTicks < 0) {
			cTicks = maxTicks;
		}
		mainIndex = Math.min(31, (32 * (int) cTicks / (int) maxTicks));
		setTexture(this, mainIndex);

		float xp = 1;
		float xm = 1;
		float zp = 1;
		float zm = 1;

		float div = ((float) maxLength * (float) cTicks / (float) maxTicks) / (float) blows;
		float indDiv = Math.min(31, ((float) 32 * (float) cTicks / (float) maxTicks) / (float) blows);

		float blastRadius = 3;
		SimpleVector ellips = new SimpleVector(blastRadius, blastRadius, blastRadius);
		float range = (float) blows * div;

		// xp
		float mt = Math.min(maxXP, range);
		SimpleVector trans = new SimpleVector(mt, 0, 0);
		SimpleVector xpm = checkForCollisionEllipsoid(trans, ellips, 1);
		if (xpm.equals(trans)) {
			maxXP = 1000;
		} else {
			maxXP = mt;
		}

		// xm
		mt = Math.min(maxXM, range);
		trans = new SimpleVector(-mt, 0, 0);
		xpm = checkForCollisionEllipsoid(trans, ellips, 1);
		if (xpm.equals(trans)) {
			maxXM = 1000;
		} else {
			maxXM = mt;
		}

		// zp
		mt = Math.min(maxZP, range);
		trans = new SimpleVector(0, 0, mt);
		xpm = checkForCollisionEllipsoid(trans, ellips, 1);
		if (xpm.equals(trans)) {
			maxZP = 1000;
		} else {
			maxZP = mt;
		}

		// zm
		mt = Math.min(maxZM, range);
		trans = new SimpleVector(0, 0, -mt);
		xpm = checkForCollisionEllipsoid(trans, ellips, 1);
		if (xpm.equals(trans)) {
			maxZM = 1000;
		} else {
			maxZM = mt;
		}

		for (int i = 0; i < blows << 2; i++) {
			blow[i].getTranslationMatrix().setIdentity();
			blow[i].translate(getTranslation());

			int p = i % 4;
			if (p == 0) {
				// xp
				blow[i].translate(Math.min(maxXP, div * xp), 0, 0);
				setTexture(blow[i], (int) Math.min(31, (blows + 1 - xp) * indDiv));
				xp++;
			}
			if (p == 1) {
				// xm
				blow[i].translate(-Math.min(maxXM, div * xm), 0, 0);
				setTexture(blow[i], (int) Math.min(31, (blows + 1 - xm) * indDiv));
				xm++;
			}
			if (p == 2) {
				// zp
				blow[i].translate(0, 0, Math.min(maxZP, div * zp));
				setTexture(blow[i], (int) Math.min(31, (blows + 1 - zp) * indDiv));
				zp++;
			}
			if (p == 3) {
				// zm
				blow[i].translate(0, 0, -Math.min(maxZM, div * zm));
				setTexture(blow[i], (int) Math.min(31, (blows + 1 - zm) * indDiv));
				zm++;
			}
		}

		if (cTicks > maxTicks) {
			disable();
			return true;
		}
		return false;
	}

	private void setTexture(Object3D obj, int index) {

		if (index > 15) {
			index = 15 + (16 - index);
		}

		int v = (index / 4) * 64;
		int u = (index % 4) * 64;

		PolygonManager pm = obj.getPolygonManager();
		int tid = pm.getPolygonTexture(0);
		Texture t = TextureManager.getInstance().getTextureByID(tid);
		float u1 = (float) u / (float) t.getWidth();
		float v1 = (float) v / (float) t.getHeight();
		float u2 = (float) (u + 64) / (float) t.getWidth();
		float v2 = (float) (v + 64) / (float) t.getHeight();
		TextureInfo ti = new TextureInfo(tid, u1, v1, u1, v2, u2, v1);
		pm.setPolygonTexture(0, ti);
		ti = new TextureInfo(tid, u1, v2, u2, v2, u2, v1);
		pm.setPolygonTexture(1, ti);
		obj.recreateTextureCoords();
	}
}
