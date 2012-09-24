package robombs.game.view;

import java.util.*;

import robombs.game.model.*;
import robombs.game.*;

import com.threed.jpct.*;
import com.threed.jpct.util.*;

/**
 * A manager class for handling client objects. This manager holds references to
 * all client objects that this client knows of (even remote ones).
 */
public class ClientObjectManager {

	static final private long serialVersionUID = 1L;

	private Map<LocalObject, ClientObject> objects = null;
	private ClientObjectFactory cFac = ClientObjectFactory.getInstance();
	private ShadowHelper sh = null;
	private boolean forBotClient = false;

	/**
	 * Creates a new manager.
	 */
	public ClientObjectManager(ShadowHelper sh, boolean forBotClient) {
		objects = new HashMap<LocalObject, ClientObject>();
		this.sh = sh;
		this.forBotClient = forBotClient;
		Colorizer.getInstance().init();
	}

	/**
	 * Returns all client objects that this manager knows of.
	 * 
	 * @return Collection the client objects
	 */
	public Collection<ClientObject> getClientObjects() {
		return objects.values();
	}

	/**
	 * Returns the underlying data model of a view object.
	 * 
	 * @param co
	 *            the view object
	 * @return LocalObject the underlying local object that the view object
	 *         represents
	 */
	public LocalObject getLocalObjectToView(ClientObject co) {
		for (Iterator<Map.Entry<LocalObject, ClientObject>> itty = objects.entrySet().iterator(); itty.hasNext();) {
			Map.Entry<LocalObject, ClientObject> entry = itty.next();
			if (co.equals(entry.getValue())) {
				return (LocalObject) entry.getKey();
			}
		}
		return null;
	}

	/**
	 * Gets a client object that has been created on a remote client with a
	 * given id. If no such object exists, it returns null.
	 * 
	 * @param objID
	 *            the id of the object
	 * @param clientID
	 *            the id of the client
	 * @return ClientObject the object or null
	 */
	public ClientObject getClientObject(int objID, int clientID) {
		LocalObject dummy = new LocalObject(clientID);
		dummy.setObjectID(objID);
		if (objects.containsKey(dummy)) {
			return (ClientObject) objects.get(dummy);
		}
		return null;
	}

	public void setShadowHelper(ShadowHelper sh) {
		this.sh = sh;
	}

	public List<SimpleVector> getPlayerPositions(boolean livingOnly, ClientObject self) {
		int myTeam = TeamAssigner.getTeam(self.getClientID(), self.getObjectID());
		if (myTeam == 0) {
			return getPlayerPositions(livingOnly);
		}
		List<SimpleVector> res = new ArrayList<SimpleVector>();
		for (ClientObject co : objects.values()) {
			if (co instanceof EnemyView && (!livingOnly || co.getBackAnimation() != Animations.DEAD) && TeamAssigner.getTeam(co.getClientID(), co.getObjectID()) != myTeam) {
				// Include both, just to be sure...
				res.add(co.getBackPosition());
				res.add(co.getTranslation());
			}
		}
		return res;
	}

	public List<SimpleVector> getPlayerPositions(boolean livingOnly) {
		List<SimpleVector> res = new ArrayList<SimpleVector>();
		for (ClientObject co : objects.values()) {
			if (co instanceof EnemyView && (!livingOnly || co.getBackAnimation() != Animations.DEAD)) {
				// Include both, just to be sure...
				res.add(co.getBackPosition());
				res.add(co.getTranslation());
			}
		}
		return res;
	}

	public int getPlayerCount() {
		int cnt = 0;
		for (ClientObject co : objects.values()) {
			// A player with a very low y-position is actually there, but not
			// placed on the floor.
			// Such a player should be considered as absent.
			if (co instanceof EnemyView && co.getTranslation().y > Globals.skyLimit && co.getBackPosition().y > Globals.skyLimit) {
				cnt++;
			}
		}
		return cnt;
	}

	public void enterCollisionMode() {
		for (ClientObject co : objects.values()) {
			co.enterCollisionMode();
		}
	}

	public void leaveCollisionMode() {
		for (ClientObject co : objects.values()) {
			co.leaveCollisionMode();
		}
	}

	/**
	 * Returns the view object for a given local object. If the manager already
	 * knows of this object, it returns that one. If it doesn't, it creates a
	 * new one and returns that.
	 * 
	 * @param lo
	 *            the local object on which the view is based
	 * @param world
	 *            the current world (needed, because the manager has to add the
	 *            new view object to it)
	 * @param bulMan
	 *            the manager for the local bullets (because collisions of local
	 *            bullets with this object has be determined)
	 * @return ClientObject the object
	 */
	public ClientObject getOrCreateClientObject(LocalObject lo, World world, ClientEventQueue eventQueue) {
		ClientObject co = objects.get(lo);
		if (co != null) {
			co.touch();
			co.setToLocalObject(lo);
			co.setRemoteFlag(true);
			co.setTransfered(true);
		} else {
			co = cFac.create(lo.getType());
			co.setEventQueue(eventQueue);
			// System.out.println(lo.getType()+"/"+lo.getPosition());
			co.addCollisionListener(new ClientObjectCollisionListener(lo, false));
			co.touch();
			co.setToLocalObject(lo);
			co.setRemoteFlag(true);
			co.setTransfered(true);
			co.initTransformation();
			co.addToWorld(world);
			objects.put(lo, co);

			if (!forBotClient) {
				if (lo.getType() == Types.PLAYER) {
					Colorizer.getInstance().colorize(co, lo);
				}
			}

			if (sh != null) {
				sh.addCaster(co);
			}
		}
		return co;
	}

	public static ClientObject createLocalPlayerObject(LocalObject lo, World world, ClientEventQueue eventQueue) {
		// Der Player muss nicht vom Manager verwaltet werden. Er wird hier nur
		// erzeugt.
		ClientObject co = ClientObjectFactory.getInstance().create(Types.LOCAL_PLAYER);
		co.setEventQueue(eventQueue);
		co.addToWorld(world);
		co.setRemoteFlag(false);
		co.setAsLocalOnly();
		co.setVisibility(true);
		return co;
	}

	/**
	 * Removes all objects from the manager and the world.
	 * 
	 * @param world
	 *            the current world
	 */
	public void removeAll(World world) {
		List<ClientObject> tmp = new ArrayList<ClientObject>(objects.values());
		for (Iterator<ClientObject> itty = tmp.iterator(); itty.hasNext();) {
			ClientObject co = itty.next();
			if (!co.isLocalOnly()) {
				remove(co, world);
				if (sh != null) {
					sh.removeCaster(co);
				}
			}
		}
	}

	public void process(long ticks, Level level, World world, boolean received, boolean botUsage) {
		List<ClientObject> toKick = new ArrayList<ClientObject>();
		for (ClientObject obj : getClientObjects()) {
			if (obj.isTransfered() || !received) {
				if (!botUsage) {
					obj.process(ticks, level);
				} else {
					obj.processForBotClient(ticks, level);
				}
			} else {
				if (received && !obj.isLocalOnly()) {
					toKick.add(obj);
				}
			}
		}
		for (ClientObject co : toKick) {
			remove(co, world);
			if (sh != null) {
				sh.removeCaster(co);
			}
		}
	}

	/**
	 * Removes a single object from the manager and the world.
	 * 
	 * @param co
	 *            the object to remove
	 * @param world
	 *            the world
	 */
	public void remove(ClientObject co, World world) {
		co.removeFromWorld(world);
		Set<Map.Entry<LocalObject, ClientObject>> entries = objects.entrySet();
		for (Iterator<Map.Entry<LocalObject, ClientObject>> itty = entries.iterator(); itty.hasNext();) {
			Map.Entry<LocalObject, ClientObject> es = itty.next();
			if (es.getValue().equals(co)) {
				itty.remove();
				if (sh != null) {
					sh.removeCaster(co);
				}
			}
		}
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();

		for (ClientObject obj : getClientObjects()) {
			sb.append(obj.getClass().getName() + " - " + obj.getBackPosition() + " - " + obj.getID());
		}

		return sb.toString();
	}

}
