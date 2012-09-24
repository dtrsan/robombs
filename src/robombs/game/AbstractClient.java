package robombs.game;

import robombs.game.model.*;
import robombs.game.util.Ticker;
import robombs.game.view.*;
import robombs.clientserver.*;

import java.util.*;

import com.threed.jpct.*;

/**
 * An abstract base class for client. There is a visual client (the one that you
 * are using when playing the game) and a bot client. The bot client holds a
 * simplified version of the game world and has no display methods.
 * 
 * @author EgonOlsen
 * 
 */
public abstract class AbstractClient {

	protected final Object SYNC = new Object();
	protected final Object KILL_SYNC = new Object();

	protected ClientObjectManager coMan = null;
	protected Ticker timeout = null;

	protected NetState state = new NetState();
	protected Ticker waitTimer = new Ticker(1000);
	protected SimpleClient clientImpl = null;
	protected ClientEventQueue eventQueue = new ClientEventQueue();

	protected Level level = null;
	protected World world = new World();

	protected Ticker fireTicker = null;
	protected Ticker bombTicker = null;
	protected List<LocalObject> toKill = new ArrayList<LocalObject>();

	protected LocalBombManager bombMan = null;

	protected BlueThunderServer serverImpl = null;

	protected PlayerDummy dummy = null;

	protected boolean quit = false;
	protected boolean received = false;
	protected boolean shouldBeConnected = false;

	protected boolean firstTransferFinished = false;
	protected boolean entitiesUpdated = false;

	protected boolean isReady = false;
	protected boolean disConnectScheduled = false;

	protected MapList mapList = null;
	protected List<MapInfo> selectedMaps = new ArrayList<MapInfo>();

	protected boolean reloadScheduled = false;
	protected boolean roundCompleted = false;

	private int currentLevel = 0;
	protected volatile boolean modified = false;

	protected int playerCount = 0;
	protected int respawnCount = 0;

	protected WaterDropper dropper = new WaterDropper();

	protected float orgFOV = world.getCamera().getFOV();
	protected float orgYFOV = world.getCamera().getYFOV();

	protected volatile boolean respawnRunning = false;

	protected SimpleVector[] spawnPoints = new SimpleVector[] { new SimpleVector(60, -10, 100), new SimpleVector(40, -10, 100), new SimpleVector(60, -10, 120),
			new SimpleVector(40, -10, 120) };

	protected boolean isFree(SimpleVector pos, CrateManager cm, LocalBombManager lbm, ClientObjectManager com) {
		List<SimpleVector> poss = new ArrayList<SimpleVector>(cm.getCratePositions());
		poss.addAll(lbm.getObjectPositions());
		poss.addAll(com.getPlayerPositions(true));
		for (SimpleVector pos2 : poss) {
			float delta = pos.calcSub(pos2).length();
			if (delta < 4f) {
				NetLogger.log("Client: Position " + pos + " is blocked by another entity!");
				return false;
			}
		}
		return true;
	}

	protected boolean allPlayersHaveSpawned() {
		return respawnCount >= playerCount;
	}

	protected void checkWaitingState() {
		if (state.getState() == NetState.STATE_WAITING && waitTimer.getTicks() > 0) {
			// Somehow, the server didn't get it....try again...
			// This shouldn't be needed anymore as it obviously was a
			// synchronization problem, but one never knows...
			Event ev = new Event(Event.LEVEL_LOADED, -99, -99, clientImpl.getClientID());
			ev.setSourceClientID(clientImpl.getClientID());
			eventQueue.add(ev);
			NetLogger.log("Client " + getClientID() + ": Retransmitting state!");
		}
	}

	public void ready(boolean isReady) {
		int m = Event.PLAYER_READY;
		if (!isReady) {
			m = Event.PLAYER_NOT_READY;
		}
		Event event = new Event(m, -99, -99, clientImpl.getClientID());
		event.setSourceClientID(clientImpl.getClientID());
		synchronized (SYNC) {
			eventQueue.add(event);
		}
		this.isReady = isReady;
	}

	public boolean isReady() {
		return isReady;
	}

	public int getClientID() {
		if (clientImpl == null) {
			return 0;
		}
		return clientImpl.getClientID();
	}

	public boolean isConnected() {
		return clientImpl != null && clientImpl.isConnected();
	}

	public int getMapNumber() {
		return currentLevel;
	}

	public void nextMap() {
		currentLevel++;
		currentLevel = currentLevel % selectedMaps.size();
	}

	public void firstMap() {
		currentLevel = 0;
	}

	public NetState getState() {
		return state;
	}

	public List<MapInfo> getSelectedMaps() {
		return selectedMaps;
	}

	public void beforeSending() {
		synchronized (SYNC) {
			if (modified) {
				modified = false;
				prepareClientData();
			}
		}
	}

	public void beforeReceiving() {
		received = false;
		if (coMan != null) {
			// Erstmal alle als nicht übertragen markieren. Sollten sie es
			// dennoch werden, wird
			// unten umgesetzt. Ansonsten fliegen sie beim nächsten Zeichnen!
			synchronized (SYNC) {
				for (ClientObject co : coMan.getClientObjects()) {
					co.setTransfered(false);
				}
			}
		}
	}

	public void afterReceiving() {
		if (state.getState() == NetState.STATE_RUNNING) {
			firstTransferFinished = true;
		}
	}

	public void dataReceivedEnd() {
		received = true;
	}

	public DataContainer[] dataReceived(DataContainer c, int type) {
		if (c != null && c.hasData()) {

			synchronized (SYNC) {
				if (clientImpl != null) {
					if (coMan != null) {
						if (type == MessageTypes.OBJ_TRANSFER) {
							ExtendedDataContainer ec = new ExtendedDataContainer(c);
							synchronized (world) {
								while (ec.hasData()) {
									LocalObject lo = ec.getLocalObject();
									if (lo.getClientID() != clientImpl.getClientID()) {
										// Somebody else...
										coMan.getOrCreateClientObject(lo, world, eventQueue);
									}
								}
							}
						}
					}

					if (type == MessageTypes.EVENT) {
						EventDataContainer edc = new EventDataContainer(c);
						int cnt = 0;
						synchronized (world) {
							while (edc.hasData()) {
								Event event = edc.getEvent();
								processEvent(event);
								cnt++;
							}
						}
					}

					if (type == MessageTypes.IMPORTANT_INFO) {
						InfoDataContainer idc = new InfoDataContainer(c);
						int cnt = 0;
						while (idc.hasData()) {
							InfoLine il = idc.getInfoLine();
							processInfo(il);
							cnt++;
						}
					}
				}
			}
		}
		return null;
	}

	/**
	 * Checks for a time out of other clients. If a remote client can't transmit
	 * any data, it can't transmit that it's gone either. This is handled here
	 * by removing objects from clients that seem to be lost.
	 * 
	 * @throws Exception
	 */
	protected void checkTimeout() throws Exception {
		if (timeout.getTicks() > 0) {
			Collection<ClientObject> objs = coMan.getClientObjects();
			List<ClientObject> toKill = new ArrayList<ClientObject>();
			for (ClientObject co : objs) {
				if (co.isOld()) {
					toKill.add(co);
				}
			}
			for (ClientObject co : toKill) {
				coMan.remove(co, world);
			}
		}

		if (clientImpl != null && !clientImpl.isConnected()) {
			// Damn! Where's our connection gone?
			disconnect();
		}
	}

	/**
	 * Removes every object from the world that's part of the toKill-List.
	 */
	protected void cleanUp() {
		synchronized (KILL_SYNC) {
			for (Iterator<LocalObject> itty = toKill.iterator(); itty.hasNext();) {
				LocalObject obj = itty.next();
				if (obj.getView() != null) {
					ClientObject view = obj.getView();
					view.removeFromWorld(world);
				}
			}
			toKill.clear();
		}
	}

	protected void updateExplosions(long ticks) {
		level.getExplosionManager().process(ticks);
	}

	/**
	 * Updates all entities (like player meshes) that are remote, i.e. that came
	 * from the server.
	 * 
	 * @param ticks
	 *            long the number of ticks passed
	 */
	protected void updateRemoteEntities(long ticks, boolean botUsage) {
		if (coMan != null) {
			coMan.process(ticks, level, world, received, botUsage);
			if (firstTransferFinished) {
				entitiesUpdated = true;
			}
		}
	}

	protected void updateCrates(long ticks) {
		level.getCrateManager().process(ticks, level);
		level.getItemManager().process(ticks, level);
	}

	public void createSpawnPoints() {
		SimpleVector[] points = level.getSpawnPoints();
		if (points != null && points.length != 0) {
			spawnPoints = points;
		}
	}

	/**
	 * Taken from the jpct forums, posted by zammbi
	 * 
	 * @param m
	 * @return
	 */
	protected final SimpleVector matrixToRad(Matrix m) {
		float[] mDump = m.getDump();

		float heading, attitude, bank;
		if (mDump[4] > 0.998) { // singularity at north pole
			heading = (float) Math.atan2(mDump[2], mDump[10]);
			attitude = (float) (Math.PI / 2);
			bank = 0;
		} else if (mDump[4] < -0.998) { // singularity at south pole
			heading = (float) Math.atan2(mDump[2], mDump[10]);
			attitude = (float) (-Math.PI / 2);
			bank = 0;
		} else {
			heading = (float) Math.atan2(-mDump[8], mDump[0]);
			bank = (float) Math.atan2(-mDump[6], mDump[5]);
			attitude = (float) Math.asin(mDump[4]);
		}
		return new SimpleVector(bank, heading, attitude);
	}

	abstract public void disconnect() throws Exception;

	abstract protected void processInfo(InfoLine il);

	abstract protected void processEvent(Event ev);

	abstract protected void prepareClientData();

}
