package robombs.game.view;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import robombs.clientserver.NetLogger;
import robombs.game.Globals;
import robombs.game.util.Ticker;

import com.threed.jpct.Object3D;
import com.threed.jpct.SimpleVector;
import com.threed.jpct.World;

/**
 * A simple manager for smoke.
 */
public class SmokeCloudManager {

	private SmokeCloud[] parts = null;
	private World level = null;
	private long lastCleanup = 0;
	private int cnt = 100;
	private Set<SmokeEmitter> emitters = new HashSet<SmokeEmitter>();

	/**
	 * Creates a new manager with a default number of particles (100).
	 * 
	 * @param level
	 *            World the world
	 */
	public SmokeCloudManager(World level) {
		this.level = level;
		init();
	}

	/**
	 * Creates a new manager for a number of particles.
	 * 
	 * @param level
	 *            World the world
	 * @param max
	 *            int the number of particles
	 */
	public SmokeCloudManager(World level, int max) {
		cnt = max;
		this.level = level;
		init();
	}

	public void addSmokeEmitter(SimpleVector pos) {
		int freq = 5;
		int size = 15;
		if (Globals.enhancedGraphics) {
			freq = 3;
			size = 22;
		}
		SmokeEmitter emitter = new SmokeEmitter(pos, 500, freq, size);
		emitters.add(emitter);
	}

	/**
	 * Moves all clouds managed by this manager.
	 * 
	 * @param ticks
	 *            long the number of ticks passed since the last call
	 */
	public void move(long ticks) {

		for (Iterator<SmokeEmitter> itty = emitters.iterator(); itty.hasNext();) {
			SmokeEmitter emitter = itty.next();
			if (emitter.isOld()) {
				itty.remove();
			} else {
				emitter.smoke(ticks, this);
			}
		}

		for (int i = 0; i < cnt; i++) {
			SmokeCloud pp = parts[i];
			if (pp != null && pp.getVisibility()) {
				pp.move(ticks);
			}
		}
		doCleanup();
	}

	/**
	 * Adds a new particle to the manager. If the maximum number of active,
	 * visible particles have been reached, nothing happens.
	 * 
	 * @param pos
	 *            SimpleVector the position
	 * @param vel
	 *            SimpleVector the velocity
	 * @param time
	 *            long the time-limit
	 * @param texture
	 *            String the texture's name
	 */
	public void addSmokeCloud(SimpleVector pos, SimpleVector vel, long time, String texture, float scale) {
		SmokeCloud p = getSmokeCloud();
		if (p != null) {
			p.setScale(scale);
			p.setOrigin(pos);
			p.setVelocity(vel);
			p.setLifeTime(time);
			p.setTexture(texture);
			p.setTransparencyMode(Object3D.TRANSPARENCY_MODE_ADD);
			p.reset();
		}
	}

	/**
	 * Initializes the manager
	 */
	private void init() {
		parts = new SmokeCloud[cnt];
		lastCleanup = Ticker.getTime();
	}

	/**
	 * Every 30 secs, the managers checks for old clouds that can be cleaned up.
	 */
	private void doCleanup() {
		long time = Ticker.getTime() - lastCleanup;
		if (time > 30000 || time < 0) {
			int counter = 0;
			for (int i = cnt - 1; i >= 0; i--) {
				SmokeCloud pp = parts[i];
				if (pp != null && pp.isOld()) {
					level.removeObject(pp);
					parts[i] = null;
					counter++;
				}
			}
			if (counter != 0) {
				NetLogger.log("Smoke cloud system cleanup performed: " + counter + " clouds removed!");
			}
			lastCleanup = Ticker.getTime();
		}
	}

	/**
	 * Gets a particle from the manager's pool. If the pool if full of
	 * particles, null will be returned.
	 * 
	 * @return Particle a fresh particle or null
	 */
	private SmokeCloud getSmokeCloud() {
		int np = -1;
		for (int i = 0; i < cnt; i++) {
			SmokeCloud pp = parts[i];
			if (pp != null && !pp.getVisibility()) {
				pp.setVisibility(Object3D.OBJ_VISIBLE);
				return pp;
			}
			if (pp == null && np == -1) {
				np = i;
			}
		}

		SmokeCloud p = null;

		int s = 0;
		if (np != -1) {
			s = np;
		}

		for (int i = s; i < cnt; i++) {
			if (parts[i] == null) {
				p = new SmokeCloud();
				level.addObject(p);
				parts[i] = p;
				break;
			}
		}
		return p;
	}
}
