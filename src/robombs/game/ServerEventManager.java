package robombs.game;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import robombs.clientserver.ClientInfo;
import robombs.clientserver.DataContainer;
import robombs.clientserver.NetGlobals;
import robombs.clientserver.NetLogger;
import robombs.game.model.LocalBombManager;
import robombs.game.model.LocalObject;
import robombs.game.model.PlayerInfo;
import robombs.game.model.ServerObjectManager;
import robombs.game.model.Types;

import com.threed.jpct.SimpleVector;

/**
 * A helper class for the server to ease event processing. An event is send from
 * the client to the server and processed there. This is what this class does.
 */
public class ServerEventManager {

	private volatile boolean threadRunning = false;
	private volatile boolean drawDetectionDone = false;
	private Set<Integer> collectedItems = new HashSet<Integer>();

	/**
	 * Manages an event.
	 * 
	 * @param event
	 *            Event the event to manage
	 * @param servMan
	 *            ServerObjectManager the server object manager that lives on
	 *            this server
	 * @param server
	 *            BlueThunderServer the actual server instance
	 * @param ci
	 *            ClientInfo the client info of the client from which the event
	 *            came from
	 * @return DataContainer[] an optional response. May be null (and usually is
	 *         in the current implementation).
	 */
	public DataContainer[] manageEvent(Event event, ServerObjectManager servMan, BlueThunderServer server, ClientInfo ci) {
		switch (event.getType()) {
		case Event.BOMB_OVERLOADED:
			return processBombOverload(event, servMan, server);
		case Event.EXPLOSION_HIT:
			return processExplosionHit(event, servMan, server);
		case Event.CRATE_HIT:
			return processCrateHit(event, servMan, server);
		case Event.BOMB_ITEM_COLLECTED:
			return processBombItem(event, servMan, server);
		case Event.FIREPOWER_ITEM_COLLECTED:
			return processFirepowerItem(event, servMan, server);
		case Event.KICK_ITEM_COLLECTED:
			return processKickItem(event, servMan, server);
		case Event.DISEASE_ITEM_COLLECTED:
			return processDiseaseItem(event, servMan, server);
		case Event.ENTITY_REMOVE:
			return processEntityRemoval(event, servMan, ci, server);
		case Event.FIRE:
			return processFire(event, server);
		case Event.PLACE_BOMB:
			return processBomb(event, server);
		case Event.BOMB_DISABLED:
			return processBombDisabled(event, servMan, server);
		case Event.BOMB_TOUCHED:
			return processBombKicked(event, servMan, server);
		case Event.PLAYER_RESPAWNED:
			return processRespawn(event, servMan, server);
		case Event.PLAYER_READY:
		case Event.PLAYER_NOT_READY:
			return processPlayerState(event, server);
		case Event.LEVEL_LOADED:
			return processLoadingState(event, server);
		case Event.NEXT_LEVEL_REQUEST:
			return processLevelRequest(event, server);
		case Event.ROUND_LOST:
			return processRoundLost(event, servMan, server);
		case Event.TAUNT:
			return processTaunt(event, server);
		case Event.TEAM_SET:
			return processTeamSet(event, server);
		default:
			return null;
		}
	}

	private DataContainer[] processTeamSet(Event event, BlueThunderServer server) {
		// *shiver*
		SimpleVector abusedData=event.getOrigin();
		int team=(int) abusedData.x;
		int cid=(int) abusedData.y;
		int oid=(int) abusedData.z;
		
		InfoDataContainer idc = new InfoDataContainer();
		InfoLine il = new InfoLine(InfoLine.TEAM_ASSIGNED, team, String.valueOf(cid), String.valueOf(oid));
		idc.add(il);
		server.broadcast(idc);
		return null;
	}

	public synchronized void clearCollectedItems() {
		collectedItems.clear();
	}

	public synchronized void revivePlayers(BlueThunderServer server) {
		List<PlayerInfo> pis = server.getPlayers();
		for (PlayerInfo pi : pis) {
			pi.revive();
		}
	}

	private int getAliveCount(BlueThunderServer server) {
		List<PlayerInfo> pis = server.getPlayers();
		int aliveCnt = 0;
		for (PlayerInfo pi : pis) {
			if (!pi.isDead()) {
				aliveCnt++;
			}
		}
		return aliveCnt;
	}

	public synchronized DataContainer[] processRoundLost(final Event event, ServerObjectManager servMan, final BlueThunderServer server) {
		PlayerInfo sip = server.getPlayerInfo(event.getSourceClientID(), event.getSourceID());
		sip.kill();
		final List<PlayerInfo> pis = server.getPlayers();
		int aliveCnt = 0;
		int botCnt = 0;
		PlayerInfo alive = null;

		// System.out.println(sip.getName()+" is dead!");

		int[] teamAliveCount = new int[5];

		for (PlayerInfo pi : pis) {
			// System.out.println(pi.getClientID()+"/"+pi.getName()+"/"+pi.isDead());
			if (!pi.isDead()) {
				teamAliveCount[TeamAssigner.getTeam(pi)]++;
				aliveCnt++;
				alive = pi;
				if (pi.isBot()) {
					botCnt++;
				}
			}
		}

		boolean onlyOneTeamLeft = false;
		int teamCount = 0;
		int team = 0;
		for (int i = 0; i < teamAliveCount.length; i++) {
			if (teamAliveCount[i] != 0) {
				teamCount++;
				team = i;
			}
		}

		String value = "";
		if (teamCount == 1 && teamAliveCount[0] == 0) {
			onlyOneTeamLeft = true;
			value = "Team " + team;

		}

		if (aliveCnt <= 1 || onlyOneTeamLeft && !threadRunning) {
			// Level ended!
			threadRunning = true;
			final PlayerInfo winner = alive;
			final boolean onlyOneTeamLeftf = onlyOneTeamLeft;
			final String valuef = value;
			final int teamf = team;
			new Thread() {
				// Start a new thread that sends a new event after some time has
				// passed.
				public void run() {
					try {
						Thread.sleep(300); // Sleep some time to detect a draw!

						PlayerInfo win = winner;

						if (getAliveCount(server) == 0) {
							// Somehow the winner got lost on the way?
							win = null;
						}

						// After this, no event shall kill the player anymore...
						drawDetectionDone = true;

						String name = "";
						if (win != null) {
							name = win.getName();
							win.won();
						}

						for (PlayerInfo pi : pis) {
							if (pi != win) {
								if (onlyOneTeamLeftf && TeamAssigner.getTeam(pi) == teamf) {
									pi.won();
								} else {
									pi.lost();
								}
							}
						}

						InfoDataContainer dc = new InfoDataContainer();
						InfoLine il = new InfoLine(InfoLine.ROUND_COMPLETED, onlyOneTeamLeftf ? teamf : 0, name, valuef);
						dc.add(il);
						server.broadcast(dc);

						InfoDataContainer idc = new InfoDataContainer();
						server.addScores(idc);
						server.broadcast(idc);

						Thread.sleep(Globals.ROUND_COMPLETED_TIME); // Wait...

						processLevelRequest(event, server); // Send a
						// "next-level-event"
						Thread.sleep(300); // Give some time for the client to
						// get the event!
					} catch (Exception e) {
						NetLogger.log("'Round lost' processing failed!!!");
						e.printStackTrace();
					}
					threadRunning = false;
					drawDetectionDone = false;
				}
			}.start();
		} else {
			if (aliveCnt == botCnt && !threadRunning && botCnt != 0) {
				// Only bots have survived? Accerate the game...:-)
				EventDataContainer edc = new EventDataContainer();
				LocalObject target = servMan.getLocalObjectToIDs(event.getTargetID(), event.getTargetClientID());
				Event ev = new Event(Event.ONLY_BOTS_LEFT, null, target); // Actually,
				// the
				// target
				// doesn't
				// matter...
				edc.add(ev);
				server.broadcast(edc);
			}
		}
		return null;
	}

	public DataContainer[] processLoadingState(Event event, BlueThunderServer server) {
		server.setState(event.getSourceClientID(), NetState.STATE_LEVEL_LOADED);
		if (server.getClientCount() == server.getStateCount(NetState.STATE_LEVEL_LOADED)) {
			server.reset();
			// All done? Then start the game...
			NetLogger.log("Server: All " + server.getClientCount() + " clients are ready to go!");
			InfoDataContainer dc = new InfoDataContainer();
			InfoLine il = new InfoLine(InfoLine.ALL_CLIENTS_READY, 0, "playerCount", Integer.toString(server.getPlayers().size()));
			dc.add(il);
			server.broadcast(dc);
			server.setTimeOut(NetGlobals.serverTimeOut);
			clearCollectedItems();
			revivePlayers(server);
		} else {
			NetLogger.log("Server: Waiting for " + (server.getClientCount() - server.getStateCount(NetState.STATE_LEVEL_LOADED)) + " to complete!");
		}
		return null;
	}

	public DataContainer[] processPlayerState(Event event, BlueThunderServer server) {
		if (event.getType() == Event.PLAYER_READY) {
			server.setState(event.getSourceClientID(), NetState.STATE_READY);
			readyPlayerForClient(event.getSourceClientID(), true, server);
		}
		if (event.getType() == Event.PLAYER_NOT_READY) {
			server.setState(event.getSourceClientID(), NetState.STATE_CONNECTED);
			readyPlayerForClient(event.getSourceClientID(), false, server);
		}
		return null;
	}

	private void readyPlayerForClient(int cid, boolean ready, BlueThunderServer server) {
		List<PlayerInfo> pis = server.getPlayerInfo(cid);
		if (pis != null) {
			for (PlayerInfo pi : pis) {
				if (pi != null) {
					pi.ready(ready);
				}
			}
		}
	}

	public DataContainer[] processLevelRequest(Event event, BlueThunderServer server) {
		server.setStateForAll(NetState.STATE_CONNECTED);
		NetLogger.log("Server: Sending 'next level signal' to all clients!");
		InfoDataContainer dc = new InfoDataContainer();
		InfoLine il = new InfoLine(InfoLine.NEXT_LEVEL, 0, "", "");
		dc.add(il);
		server.broadcast(dc);
		server.reset();
		return null;
	}

	/**
	 * Process a players respawn.
	 * 
	 * @param event
	 *            Event the event
	 * @param servMan
	 *            ServerObjectManager the server object manager that lives on
	 *            this server
	 * @param server
	 *            BlueThunderServer the actual server instance
	 * @return DataContainer[] an optional response. May be null.
	 */
	private DataContainer[] processRespawn(Event event, ServerObjectManager servMan, BlueThunderServer server) {
		LocalObject source = servMan.getLocalObjectToIDs(event.getSourceID(), event.getSourceClientID());
		if (source != null) {
			source.setValue(100); // Health to max!
			EventDataContainer edc = new EventDataContainer();
			Event ev = new Event(Event.PLAYER_RESET_HEALTH, source, source);
			ev.setOrigin(event.getOrigin());
			edc.add(ev);
			server.broadcast(edc);
			PlayerInfo sip = server.getPlayerInfo(event.getSourceClientID(), event.getSourceID());
			InfoDataContainer idc = new InfoDataContainer();
			InfoLine il = new InfoLine(InfoLine.SYSTEM_OUT, 0, "msg", "'" + sip.getName() + "' has spawned!");
			idc.add(il);
			server.broadcast(idc);
		} else {
			NetLogger.log("Server: Unable to find local object for respawn: " + event);
		}
		return null;
	}

	private DataContainer[] processTaunt(Event event, BlueThunderServer server) {
		EventDataContainer edc = new EventDataContainer();
		edc.add(event);
		server.broadcast(edc);
		return null;
	}

	/**
	 * Process firing.
	 * 
	 * @param event
	 *            Event the event
	 * @param server
	 *            BlueThunderServer the actual server instance
	 * @return DataContainer[] an optional response. May be null.
	 */
	private DataContainer[] processFire(Event event, BlueThunderServer server) {
		EventDataContainer edc = new EventDataContainer();
		edc.add(event);
		server.broadcast(edc);
		return null;
	}

	/**
	 * Process bombing.
	 * 
	 * @param event
	 *            Event the event
	 * @param server
	 *            BlueThunderServer the actual server instance
	 * @return DataContainer[] an optional response. May be null.
	 */
	private DataContainer[] processBomb(Event event, BlueThunderServer server) {
		EventDataContainer edc = new EventDataContainer();
		edc.add(event);
		server.broadcast(edc);
		return null;
	}

	private DataContainer[] processBombDisabled(Event event, ServerObjectManager servMan, BlueThunderServer server) {
		EventDataContainer edc = new EventDataContainer();
		edc.add(event);
		server.broadcast(edc);
		return null;
	}

	private DataContainer[] processBombKicked(Event event, ServerObjectManager servMan, BlueThunderServer server) {
		EventDataContainer edc = new EventDataContainer();
		edc.add(event);
		server.broadcast(edc);
		return null;
	}

	/**
	 * Process a client's request to remove an entity.
	 * 
	 * @param event
	 *            Event the event
	 * @param ci
	 *            ClientInfo the client info
	 * @param server
	 *            BlueThunderServer the server instance
	 * @return DataContainer[] an optional response. May be null.
	 */
	private DataContainer[] processEntityRemoval(Event event, ServerObjectManager servMan, ClientInfo ci, BlueThunderServer server) {
		LocalObject source = servMan.getLocalObjectToIDs(event.getSourceID(), event.getSourceClientID());
		if (source != null && source.getType() == Types.BOMB) {
			EventDataContainer edc = new EventDataContainer();
			edc.add(event);
			server.broadcast(edc);
		}
		server.remove(event.getSourceID(), event.getSourceClientID(), ci);
		return null;
	}

	private DataContainer[] processExplosionHit(Event event, ServerObjectManager servMan, BlueThunderServer server) {
		LocalObject target = servMan.getLocalObjectToIDs(event.getTargetID(), event.getTargetClientID());

		if (target != null) {

			if (target.getType() == Types.PLAYER) {
				processPlayerKilled(null, target, event, server);
			}
			if (target.getType() == Types.BOMB && (target.getValue() == LocalBombManager.VALUE_ACTIVE || target.getValue() == LocalBombManager.VALUE_DISABLED)) {
				processBomb(null, target, event, server);
			}
		} else {
			// This may happen due to a high latency of a client, that triggers
			// an event on an already removed object.
			NetLogger.log("Server can't find client object for: " + event + "/" + target);
		}
		return null;
	}

	private DataContainer[] processBombOverload(Event event, ServerObjectManager servMan, BlueThunderServer server) {
		LocalObject target = servMan.getLocalObjectToIDs(event.getTargetID(), event.getTargetClientID());

		if (target != null) {
			if (target.getType() == Types.BOMB && (target.getValue() == LocalBombManager.VALUE_ACTIVE || target.getValue() == LocalBombManager.VALUE_DISABLED)) {
				processBomb(null, target, event, server);
			}
		} else {
			// This may happen due to a high latency of a client, that triggers
			// an event on an already removed object.
			NetLogger.log("Server can't find client object for: " + event + "/" + target);
		}
		return null;
	}

	private DataContainer[] processCrateHit(Event event, ServerObjectManager servMan, BlueThunderServer server) {
		EventDataContainer edc = new EventDataContainer();
		Event eve = new Event(Event.REMOVE_CRATE, -99, event.getTargetID(), -99);// Crate
		// has
		// been
		// hit
		eve.setOrigin(event.getOrigin());
		edc.add(eve);
		server.broadcastToOthers(edc, event.getSourceClientID());
		return null;
	}

	private DataContainer[] processBombItem(Event event, ServerObjectManager servMan, BlueThunderServer server) {
		return processItem(event, servMan, server);
	}

	private DataContainer[] processFirepowerItem(Event event, ServerObjectManager servMan, BlueThunderServer server) {
		return processItem(event, servMan, server);
	}

	private DataContainer[] processKickItem(Event event, ServerObjectManager servMan, BlueThunderServer server) {
		return processItem(event, servMan, server);
	}

	private DataContainer[] processDiseaseItem(Event event, ServerObjectManager servMan, BlueThunderServer server) {
		return processItem(event, servMan, server);
	}

	private synchronized DataContainer[] processItem(Event event, ServerObjectManager servMan, BlueThunderServer server) {
		if (!collectedItems.contains(event.getTargetID())) {
			EventDataContainer edc = new EventDataContainer();
			Event eve = new Event(Event.COLLECT_ITEM, event.getSourceID(), event.getTargetID(), -99);
			eve.setSourceClientID(event.getSourceClientID());
			eve.setOrigin(event.getOrigin());
			edc.add(eve);
			server.broadcastToOthers(edc, event.getSourceClientID());
			collectedItems.add(event.getTargetID());
		}
		return null;
	}

	private void processBomb(LocalObject source, LocalObject target, Event event, BlueThunderServer server) {
		if (target.getValue() == LocalBombManager.VALUE_ACTIVE || target.getValue() == LocalBombManager.VALUE_DISABLED) {
			EventDataContainer edc = new EventDataContainer();
			Event eve = new Event(Event.BOMB_HIT, source, target);// Bombe
			// getroffen
			if (source != null) {
				eve.setSourceClientID(source.getClientID());
			} else {
				eve.setSourceClientID(-99);
			}
			edc.add(eve);
			server.sendToSingleClient(edc, target.getClientID());
		}
	}

	private void processPlayerKilled(LocalObject source, LocalObject target, Event event, BlueThunderServer server) {
		// System.out.println(drawDetectionDone+"/"+threadRunning);
		if (target.getValue() > 0 && !drawDetectionDone) { // Lebt der noch?
			target.setValue(0);
			EventDataContainer edc = new EventDataContainer();
			Event eve2 = new Event(Event.PLAYER_DEAD, source, target); // Tot!
			eve2.setOrigin(target.getPosition());
			if (source != null) {
				eve2.setSourceClientID(source.getClientID());
			} else {
				eve2.setSourceClientID(-99);
			}
			edc.add(eve2);
			PlayerInfo tip = server.getPlayerInfo(event.getTargetClientID(), event.getTargetID());
			tip.incKilled();
			InfoDataContainer idc = new InfoDataContainer();
			server.addScores(idc);
			InfoLine il = new InfoLine(InfoLine.SYSTEM_OUT, 0, "msg", "'" + tip.getName() + "' has been killed!");
			idc.add(il);
			server.broadcast(idc); // Highscores!
			server.broadcast(edc);
		}
	}

}
