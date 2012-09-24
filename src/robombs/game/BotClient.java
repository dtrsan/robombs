package robombs.game;

import java.net.InetAddress;
import java.util.*;

import robombs.clientserver.*;
import robombs.game.model.*;
import robombs.game.util.*;
import robombs.game.view.*;
import robombs.game.ai.*;

import com.threed.jpct.*;

import java.io.*;

public class BotClient extends AbstractClient implements DataTransferListener,	ClientPreProcessor, EventProcessor {

	private int botCnt=0;
	private List<Bot> players = new ArrayList<Bot>();
	private String[] names=new String[]{"Unknown soldier"};
	private boolean speedMode=false;
	private Ticker speedModeTimer=new Ticker(50);
	private Set<SimpleVector> spawnsUsed=new HashSet<SimpleVector>();

	public BotClient(BlueThunderServer serverImpl) throws Exception {
		this.serverImpl=serverImpl;
		try {
			List<String> names=new ArrayList<String>();
			SimpleStream ss=new SimpleStream("botnames.txt");
			BufferedReader br=new BufferedReader(new InputStreamReader(ss.getStream()));
			while(br.ready()) {
				String name=br.readLine().trim();
				if (name.length()>0) {
					names.add(name);
				}
			}
			br.close();
			ss.close();
			this.names=names.toArray(this.names);
		} catch(Exception e) {
			NetLogger.log("BotClient: Names file is missing!");
		}
	}

	public synchronized void addBot() {
		Bot player = new Bot();
		players.add(player);
		serverImpl.addPlayer(names[botCnt%names.length], this.getClientID(), player.getObjectID(), true);
		botCnt++;
		NetLogger.log("BotClient: Added bot number "+botCnt);
	}

	public synchronized void removeBot(PlayerInfo pi) {
		for (Iterator<Bot> itty=players.iterator(); itty.hasNext();) {
			LocalPlayerObject bot=itty.next();
			if (pi.getObjectID()==bot.getObjectID()) {
				serverImpl.removePlayer(pi);
				itty.remove();
				botCnt--;
				break;
			}
		}
	}

	public void connect(ServerEntry server) throws Exception {
		if (!disConnectScheduled) {
			try {
				synchronized (world) {
					world.removeAllObjects();
					world.removeAllLights();
					selectedMaps.clear();
				}
				state.reset();
				NetLogger.log("BotClient Connecting...");

				shouldBeConnected = true;
				firstTransferFinished = false;
				entitiesUpdated = false;
				DataContainer dc = new DataContainer();
				dc.add("BotClient");
				dc.add(mapList.getCheckSum());
				dc.add(1); // 1==isBotClient
				dc.add(0); // Not used for bots
				clientImpl = new SimpleClient(server, false, dc); // The bot client is local, no need to zip the data
				clientImpl.addListener(this);
				clientImpl.addPreProcessor(this);
				clientImpl.connect();
				eventQueue.clear();
				state.setState(NetState.STATE_CONNECTED);
				ready(true);
			} catch (Exception e) {
				if (clientImpl != null) {
					clientImpl.disconnect();
				}
				shouldBeConnected = false;
				clientImpl = null;
				throw e;
			}
		}
	}

	public int getBotCount() {
		return botCnt;
	}

	public void prepare() {
		try {
			long start=Ticker.getTime();
			state.setState(NetState.STATE_PREPARING);
			reInit();
			state.setState(NetState.STATE_LEVEL_LOADED);
			for (LocalPlayerObject player: players) {
				player.setPosition(new SimpleVector(0, Globals.skyLimit*10f, 0));
				player.setClientID(clientImpl.getClientID());
			}
			state.setState(NetState.STATE_WAITING);
			Event ev = new Event(Event.LEVEL_LOADED, -99, -99, clientImpl.getClientID());
			ev.setSourceClientID(clientImpl.getClientID());
			synchronized (SYNC) {
				eventQueue.add(ev);
			}
			for (LocalPlayerObject player: players) {
				player.setSpecialValue(Globals.LIVES);
			}
			waitTimer.reset();
			NetLogger.log("Level prepared in "+(Ticker.getTime()-start)+"ms.");

		} catch (Exception e) {
			throw new RuntimeException("Error while preparing level", e);
		}
	}

	public void begin() {
		for (LocalPlayerObject player: players) {
			player.activate();
		}
		respawn(players);
		try {
			// Give the respawn thread 100ms to breath...
			Thread.sleep(100);
		} catch (Exception e) {
		}
		state.setState(NetState.STATE_RUNNING);
	}

	public void disconnect() throws Exception {
		synchronized (SYNC) {
			state.reset();
			ready(false);
			shouldBeConnected = false;
			if (coMan != null) {
				coMan.removeAll(world);
			}
			for (LocalPlayerObject player: players) {
				player.setAlive();
				player.deactivate();
			}
			synchronized (world) {
				world.removeAllObjects();
				world.removeAllLights();
			}
			roundCompleted=false;
		}
		if (clientImpl != null) {
			clientImpl.disconnect();
			while (clientImpl.isConnected()) {
				// Wait for the disconnect to finish...
				Thread.sleep(100);
			}
			clientImpl = null;
			if (disConnectScheduled) {
				disConnectScheduled = false;
			}
			//
		}
		
		//TeamAssigner.clear();
		
		disConnectScheduled = false;
	}

	public void quit() {
		try {
			disconnect();
		} catch (Exception e) {
			// We are on our way to exit...no time for exceptions...
		}
		quit = true;
	}


	/**
	 * Runs the client.
	 * @throws Exception
	 */
	public void run() throws Exception {
		new Thread() {
			public void run() {
				try {
					mapList = new MapList();
					initBuffer();
					ServerEntry serv=new ServerEntry(null, InetAddress.getLocalHost(),serverImpl.getPort(), 0);
					try {
						connect(serv);
						addBot();
						gameLoop();
					} finally {
						NetLogger.log("BotClient: Client thread terminated!");
					}
				} catch(Exception e) {
					throw new RuntimeException(e);
				}
			}
		}.start();
	}


	/**
	 * Processes an info message from the server.
	 * @param il InfoLine the info
	 */
	protected void processInfo(InfoLine il) {
		if (il.getType() == InfoLine.SERVER_STARTED_GAME
				&& state.getState() <= NetState.STATE_WAITING) {
			NetLogger.log("Server has started the game!");
			reloadScheduled = true;
		}

		if (il.getType() == InfoLine.NEXT_LEVEL
				&& state.getState() == NetState.STATE_RUNNING) {
			NetLogger.log("Server switches to the next level!");
			nextMap();
			reloadScheduled = true;
		}

		if (il.getType() == InfoLine.ALL_CLIENTS_READY
				&& state.getState() == NetState.STATE_WAITING) {
			NetLogger.log("Client " + clientImpl.getClientID()
					+ ": All clients are ready to go!");
			playerCount=Integer.parseInt(il.getValue());
			respawnCount=0;
			begin();
		}

		if (il.getType() == InfoLine.END_GAME
				&& state.getState() == NetState.STATE_RUNNING) {
			NetLogger.log("Server wants to end game!");
			disConnectScheduled = true;
		}

		if (il.getType() == InfoLine.ROUND_COMPLETED) {
			NetLogger.log("Round completed");
			roundCompleted=true;
			speedMode=false;
		}

		if (il.getType() == InfoLine.PLAYER_REMOVED) {
			NetLogger.log("'" + il.getValue() + "' removed!");
			int cid = Integer.parseInt(il.getKey());
			if (cid == clientImpl.getClientID()) {
				// Somehow, a message arrived that this client's player has been disconnected.
				// This can be a server-side drop, we have to process it...
				try {
					disconnect();
				} catch (Exception e) {
					NetLogger.log("Unable to disconnect - already disconnected?");
				}
			}
			respawnCount++;
		}

		if (il.getType() == InfoLine.MAP_ENTRY) {
			// This logic is a bit unsafe, because it assumes, that the entries arrive in the
			// correct order. However, this is always the case and the way how the server works
			// ensures this, so it doesn't matter...
			if (il.getCount() == 0) {
				selectedMaps.clear();
				firstMap();
			}
			selectedMaps.add(new MapInfo(il.getKey(), il.getValue()));
		}

	}

	/**
	 * Processes an event from the server. An event if something like "player fires a shot" or "player has died".
	 * @param event Event the event
	 */
	public void processEvent(Event event) {
		switch (event.getType()) {
		case Event.ONLY_BOTS_LEFT:
			speedMode=true;
			NetLogger.log("BotClient: Only bots left...speeding up the game!");
			break;
		case Event.PLAYER_DAMAGE:
			if (event.getTargetClientID() == clientImpl.getClientID()) {
				// Myself
				for (LocalPlayerObject player: players) {
					if (player.getObjectID()==event.getTargetID()) {
						player.addToValue(event.getValue());
						break;
					}
				}
			}
			break;
		case Event.PLAYER_DEAD:
			if (!roundCompleted) {
				if (event.getTargetClientID() != clientImpl.getClientID()) {
					// Somebody else hit
					ClientObject obj = coMan.getClientObject(event.getTargetID(), event.getTargetClientID());
					if (obj != null) {
						// Can't be null anyway...
						obj.setCollisionMode(Object3D.COLLISION_CHECK_NONE);
					}
				} else {
					// Myself
					for (LocalPlayerObject player: players) {
						if (player.getObjectID()==event.getTargetID()) {
							player.setDead();
							player.getView().setCollisionMode(Object3D.COLLISION_CHECK_NONE);
							if (player.getSpecialValue()<=0) {
								// All lives lost? Tell the server that we are out for this round.
								Event ev = new Event(Event.ROUND_LOST, player, player);
								eventQueue.add(ev);
							}
							break;
						}
					}
				}
			}
			break;
		case Event.BOMB_HIT:
			if (event.getTargetClientID() == clientImpl.getClientID()) {
				// Created by myself
				bombMan.explode(event.getTargetID());
			} else {
				throw new RuntimeException("Client received an event for another client!?");
			}
			break;
		case Event.BOMB_DISABLED:
			if (event.getTargetClientID() == clientImpl.getClientID()) {
				// Created by myself
				bombMan.defuse(event.getTargetID());
			}
			break;
		case Event.BOMB_TOUCHED:
			boolean boom=false;
			for (LocalPlayerObject player: players) {
				if (player.isInvincible() && event.getSourceClientID()==clientImpl.getClientID() && player.getObjectID()==event.getSourceID() && !player.getPlayerPowers().canKick()) {
					// The local player is inVincible and has touched some bomb? Make it explode!
					Event eve = new Event(Event.BOMB_OVERLOADED, event.getSourceID(), event.getTargetID(), event.getTargetClientID());
					eventQueue.add(eve);
					boom=true;
					break;
				} 
			}
			if (!boom && event.getTargetClientID() == clientImpl.getClientID() /*&& player.getPlayerPowers().canKick()*/) {
				// Created by myself
				bombMan.startMoving(event.getTargetID(), event);
				break;
			}
			break;
		case Event.REMOVE_CRATE:
			level.getCrateManager().explode(event, level);
			break;
		case Event.COLLECT_ITEM:
			boolean wasLocal=false;
			for (LocalPlayerObject player: players) {
				if (player.getObjectID()==event.getSourceID()) {
					wasLocal=true;
					level.getItemManager().collect(event, level, event.getSourceClientID() == clientImpl.getClientID(), player);
					break;
				}
			}
			if (!wasLocal) {
				// Not collected by a local player? Remove it anyway.
				level.getItemManager().collect(event, level, false, null);
			}
			break;
		case Event.LOGIN_REJECTED:
			if (event.getTargetClientID() == clientImpl.getClientID()) {
				NetLogger.log("Client login rejected by server!");
				try {
					disconnect();
				} catch (Exception e) {
					NetLogger.log("Unable to disconnect from server!");
				}
			}
			break;
		case Event.FIRE:
			if (event.getSourceClientID() != clientImpl.getClientID()) {
				// Someone else has fired!
			}
			break;
		case Event.PLACE_BOMB:
			if (event.getSourceClientID() != clientImpl.getClientID()) {
				NetLogger.log("Bomb placed at " + event.getOrigin());
				GridPosition gp=bombMan.getBombMask().getGrid(event.getOrigin().x, event.getOrigin().z);
				bombMan.getBombMask().setMaskAt(gp, MapMask.BOMB);
			}
			break;
		case Event.ENTITY_REMOVE:
			if (event.getSourceClientID() != clientImpl.getClientID()) {
				// Someone else has removed a bomb...
		        ClientObject co=coMan.getClientObject(event.getSourceID(), event.getSourceClientID());
		        GridPosition gp2=null;
		        if (co!=null) {
		        	SimpleVector sv=co.getTransformedCenter();
		        	gp2=bombMan.getBombMask().getGrid(sv.x, sv.z);
		        }
				NetLogger.log("Bomb removed at " + event.getOrigin());
				GridPosition gp=bombMan.getBombMask().getGrid(event.getOrigin().x, event.getOrigin().z);
		        if (gp2!=null && !gp.equals(gp2)) {
		          // This is a little bit kludgy, but i had so many problems with bombs remaining in
		          // the grid that were long gone, that i want to be 110% sure now...:-)
		          NetLogger.log("Local and remote grid don't match...cleaning both!");
				  bombMan.getBombMask().setMaskAt(gp, MapMask.NO_BOMB);
		          bombMan.getBombMask().setMaskAt(gp2, MapMask.NO_BOMB);
		        } else {
		          bombMan.getBombMask().setMaskAt(gp, MapMask.NO_BOMB);
		        }
			}
			break;
		case Event.PLAYER_RESET_HEALTH:
			if (event.getSourceClientID() == clientImpl.getClientID()) {
				for (LocalPlayerObject player: players) {
					if (player.getObjectID()==event.getSourceID()) {
						player.setValue(100);
						break;
					}
				}
			} else {
				ClientObject obj = coMan.getClientObject(event.getSourceID(),
						event.getSourceClientID());
				if (obj != null) {
					obj.setCollisionMode(Object3D.COLLISION_CHECK_OTHERS
							| Object3D.COLLISION_CHECK_SELF);
				}
			}
			respawnCount++;

			int clients=serverImpl.getClientCount()-1; // One client is this one...
			int known=BotClient.this.coMan.getPlayerCount();
			if (clients!=known) {
				spawnsUsed.add(event.getOrigin());
				NetLogger.log("BotClient: Spawnpoint "+event.getOrigin()+" reserved for human player. Count is now: "+spawnsUsed.size());
			}

			break;
		}
	}


	/**
	 * Initializes the client.
	 */
	private void initClient() {
		coMan = new ClientObjectManager(null, true);
		bombMan = new LocalBombManager(level, null);
		timeout = new Ticker(2000);
	}

	/**
	 * Prepares the data from the client that should be send to the server.
	 */
	protected void prepareClientData() {
		if (clientImpl != null) {
			if (state.getState() >= NetState.STATE_LEVEL_LOADED) {
				ExtendedDataContainer playerAndObjects = new ExtendedDataContainer();
				for (LocalPlayerObject player: players) {
					playerAndObjects.add(player);
				}
				synchronized (KILL_SYNC) {
					toKill.addAll(bombMan.fillOrKill(playerAndObjects, eventQueue));
				}
				DataContainer edc = eventQueue.getEvents(clientImpl.getClientID());
				eventQueue.clear();
				clientImpl.setContainers(new DataContainer[] {playerAndObjects, edc });
			} else {
				// Events only..
				DataContainer edc = eventQueue.getEvents(
						clientImpl.getClientID());
				eventQueue.clear();
				clientImpl.setContainers(new DataContainer[] {
						new ExtendedDataContainer(), edc });
			}
		}
	}

	private void initBuffer() {
		Logger.setOnError(Logger.ON_ERROR_THROW_EXCEPTION);
		fireTicker = new Ticker(400);
		bombTicker = new Ticker(200);
	}



	/**
	 * Initializes the game world.
	 */
	private void initWorld() {
		dummy = new PlayerDummy(this.getClientID());
		dummy.setEventQueue(eventQueue);
		world.addObject(dummy);
		dummy.setCollisionMode(Object3D.COLLISION_CHECK_SELF);

		level.addToWorld(world);
	}

	/**
	 * Initializes the player. The local player on the client is, opposed to remote players, no real 3D object but
	 * just a data representation.
	 */
	private void initPlayers() {
		for (LocalPlayerObject player:players) {
			// Reinit
			player.reset();
		}
	}

	private void initPlayerView() {
		for (LocalPlayerObject player: players) {
			ClientObject playerView = ClientObjectManager.createLocalPlayerObject(player, world, eventQueue);
			playerView.setCollisionMode(Object3D.COLLISION_CHECK_SELF | Object3D.COLLISION_CHECK_OTHERS);
			playerView.addCollisionListener(new ClientObjectCollisionListener(player, true));
			player.setView(playerView);
			playerView.setAnimationSequence(null);
			dummy.setMaster(playerView);
		}
	}


/*
	private void fireBullet() {
		player.setAnimation(Animations.FIRE);
		player.setAnimationSpeed(20);
		if (fireTicker.getTicks() > 0) {
			LocalObject bul = bulMan.addBullet(player, world);
			Event ev = new Event(Event.FIRE, player, bul);
			ev.setOrigin(player.getPosition());
			ClientEventQueue.getInstance().add(ev);
			fireTicker.reset();
		}
	}
*/
	public void placeBomb(Bot bot) {
		if (bombTicker.getTicks() > 0) {
			if (bombMan.getCount(bot.getObjectID())<bot.getPlayerPowers().getBombCount()) {
				// Only do this, if you are allowed to...
				LocalObject bomb = bombMan.addBomb(clientImpl, bot, world, eventQueue);
				if (bomb != null) {
					Event ev = new Event(Event.PLACE_BOMB, bot, bomb);
					ev.setOrigin(new SimpleVector(bot.getPosition()));
					eventQueue.add(ev);
				}
			}
		}
	}


	/**
	 * Respawn!
	 */
	private void respawn(final List<Bot> locals) {
		if (!roundCompleted && clientImpl!=null) {
			// Do this in another thread to make sure, that the server has send its data at least once
			// before respawning.
			entitiesUpdated = false;
			respawnRunning=true;
			new Thread() {
				public void run() {
					setPriority(Thread.MAX_PRIORITY);
					SimpleVector respawn = null;
					int cnt = 0;
					try {
						boolean ok=false;
						do {
							// This should ensure that the bot client knows the positions of the other player...this
							// may not always work...we'll see...
							int clients=serverImpl.getClientCount()-1; // One client is this one...
							int known=BotClient.this.coMan.getPlayerCount();
							if (clients==known && entitiesUpdated) {
								ok=true;
							}
							if (!ok) {
								if (clients-known!=0) {
									NetLogger.log("BotClient: Waiting for "+(clients-known)+" client(s) to appear!");
								}
								Thread.sleep(125);
								cnt++;
							}
						} while(!ok && cnt < 60);
					} catch (Exception e) {
						e.printStackTrace();
					}

					if (cnt == 30) {
						respawnRunning=false;
						throw new RuntimeException("BotClient: Unable to respawn. Some remote data is missing!");
					}
					List<Event> events=new ArrayList<Event>();

					synchronized (SYNC) {
						Set<SimpleVector> used=new HashSet<SimpleVector>(spawnsUsed);
						int cnty=0;

						for (LocalPlayerObject local:players) {
							if (locals.contains(local)) { // Do this sh**t to ensure the order
								int its = 0;
								respawn=null;
								do {
									int index=((clientImpl.getClientID()-1)+cnty + its)%spawnPoints.length;
									SimpleVector s=null;
									if (index<spawnPoints.length && its<=15) {
										s = new SimpleVector(spawnPoints[index]);
									} else {
										// No defined spawn point available? Spawn anywhere on the map...

										MapMask mask=level.getMask();
										int height=mask.getHeight();
										int width=mask.getWidth();

										GridPosition gp=new GridPosition();
										boolean tFound=false;
										do {
											int newX=(int) (Math.random()*(float) width-1);
											int newY=(int) (Math.random()*(float) height-1);
											tFound=newX!=0 && newY!=0 && !mask.isBlocked(newX, newY) && !mask.isObstacle(newX, newY);
											gp.set(newX, newY);
											s=gp.convertTo3D();
											s.y=-10;
											tFound&=isFree(s, level.getCrateManager(), bombMan, BotClient.this.coMan);
											tFound&=!used.contains(s);
										} while(!tFound);
									}

									SimpleVector ss = new SimpleVector();
									ss.y=-3;
									if ((isFree(s, level.getCrateManager(), bombMan, BotClient.this.coMan) && world.checkCollisionSpherical(s, ss, 3f).equals(ss) && !used.contains(s)) || its > 15) {
										respawn = s;
										used.add(s);
									} else {
										its++;
										NetLogger.log("BotClient: "+s + " is blocked!");
									}
								} while (respawn == null);

								local.setPosition(respawn);
								local.setSpeed(new SimpleVector());
								local.getView().setCollisionMode(Object3D.COLLISION_CHECK_SELF | Object3D.COLLISION_CHECK_OTHERS);
								local.setAlive();
								local.setInvincible(true);
								Event ev = new Event(Event.PLAYER_RESPAWNED, local, local);
								events.add(ev);
							}
							cnty++;
						}
						for (Event ev:events) {
							eventQueue.add(ev);
						}
					}

					fireTicker.reset();
					respawnRunning=false;
				}
			}.start();
		}
	}


	private void checkRespawns() {

		if (respawnRunning) {
			return;
		}

		List<Bot> locals=null;
		for (Bot player:players) {
			if (player.isDead() && Ticker.hasPassed(player.deadSince(), 2000) && player.getSpecialValue() > 0 && !roundCompleted) {
				if (locals==null) {
					locals=new ArrayList<Bot>();
				}
				locals.add(player);
			}
		}
		if (locals!=null) {
			respawn(locals);
		}
	}

	/**
	 * The game loop. This is where the game...well...loops...:-)
	 * @throws Exception
	 */
	private void gameLoop() throws Exception {
		Ticker ticker = new Ticker(20);
		Ticker sec=new Ticker(10000);

		long time=0;
		long aiTime=0;

		int sleepy=(int)((1000f/((float)Globals.frameLimit/*0.075f*/))+0.5f);

		Thread.currentThread().setPriority(Thread.NORM_PRIORITY);

		while (!quit) {

			long s=Ticker.getTime();

			checkWaitingState();

			if (disConnectScheduled) {
				try {
					quit();
				} catch (Exception e) {
					// Don't care...
				}
			}

			if (reloadScheduled) {
				reloadScheduled = false;
				prepare();
			}

			int ticks = ticker.getTicks();
			if (ticks > 0) {
				if (state.getState() >= NetState.STATE_WAITING) {
					synchronized (SYNC) {
						long ait=Ticker.getTime();
						updatePlayer(ticks);
						ait=Ticker.getTime()-ait;
						aiTime+=ait;
						checkRespawns();
						updatePlayerView(ticks);
						updateRemoteEntities(ticks, true);
						updateCrates(ticks);
						updateBombs(ticks);
						checkTimeout();
						updateExplosions(ticks);
						eventQueue.processLocalEvents(this, clientImpl.getClientID());
					}
					cleanUp();
				}
				modified=true;
				if (clientImpl!=null && Globals.activeTransferForBots) {
					clientImpl.triggerTransfer();
				}
			}

			if (clientImpl != null && !clientImpl.isConnected()
					&& shouldBeConnected) {
				shouldBeConnected = false;
			}

			s=Ticker.getTime()-s;
			time+=s;

			if (sec.getTicks()>0 || s<0) {
				NetLogger.log("BotClient: Bot load is "+(int)(((float)time/10000f)*100f)+"% ("+(int)(((float)aiTime/(float)time)*100f)+"% used by bot AI)");
				time=0;
				aiTime=0;
			}

			Thread.sleep(sleepy);
		}
	}

	private void loadLevel() throws Exception {
		MapInfo mi = selectedMaps.get(getMapNumber());
		Logger.log("Loading level " + mi.getName() + "...", Logger.MESSAGE);
		level = new Level("data/levels/" + mi.getName(), mi.getSet(), world, eventQueue, null, true);
		createSpawnPoints();
	}

	private List<SimpleVector> getPositionsOtherTeams(Bot bot) {
		int myTeam=TeamAssigner.getTeam(bot);
		if (myTeam==0) {
			return getPositions();
		}
		List<SimpleVector> res=new ArrayList<SimpleVector>();
		for (LocalPlayerObject player:players) {
			if (!player.isDead() && TeamAssigner.getTeam(player)!=myTeam) {
				res.add(new SimpleVector(player.getPosition()));
			}
		}
		return res;
	}
	
	private List<SimpleVector> getPositions() {
		List<SimpleVector> res=new ArrayList<SimpleVector>();
		for (LocalPlayerObject player:players) {
			if (!player.isDead()) {
				res.add(new SimpleVector(player.getPosition()));
			}
		}
		return res;
	}

	private void updateBombs(long ticks) {
		if (speedMode && speedModeTimer.getTicks()>0 && bombMan.getCount()<15) {
			level.populateSparse(bombMan, clientImpl, world, eventQueue);
		}
		bombMan.processBotBombs(players, level, ticks);
	}

	/**
	 * Updates the local player. Albeit the local player is no mesh, this method takes care of setting the correct
	 * animation because that will be transfered to the server and all other clients.
	 * @param ticks long the number of ticks passed
	 */
	private void updatePlayer(long ticks) {
		if (state.getState() == NetState.STATE_RUNNING && allPlayersHaveSpawned()) {
			List<SimpleVector> pPos=getPositions();
			coMan.enterCollisionMode();
			bombMan.enterCollisionMode();
			bombMan.moveBombs(world, ticks);
			for (Bot player:players) {
				if (!player.isDead()) {
					List<SimpleVector> pPosTeam=getPositionsOtherTeams(player);
					ClientObject playerView=player.getView();
					playerView.setVisibility(false);
					boolean moved=false;
					moved|= player.nextStep(this, world, ticks, level, coMan, bombMan, pPos, pPosTeam, dummy);

					if (player.getPlayerPowers().isSick()==PlayerPowers.DROP_IMMEDIATELY) {
						placeBomb(player);
					}

					playerView.setVisibility(true);
					if (moved) {
						/*
						if (mouse.buttonDown(1)) {
							player.setAnimation(Animations.CROUCH_MOVE);
							player.setAnimationSpeed(20);
						} else {
						*/
						player.setAnimation(Animations.MOVE);
						player.setAnimationSpeed(40);
					} else {
						/*
						if (mouse.buttonDown(1)) {
							player.setAnimation(Animations.CROUCH_NONE);
						} else {
						*/
						player.setAnimation(Animations.NONE);
						player.setAnimationSpeed(300);
					}
				} else {
					if (Ticker.hasPassed(player.deadSince(), 1000)) {
						player.setAnimation(Animations.DEAD);
					} else {
						player.setAnimation(Animations.DIE);
					}
					player.setAnimationSpeed(80);
				}
			}
			coMan.leaveCollisionMode();
			bombMan.leaveCollisionMode();
		}
	}

	private void updatePlayerView(long ticks) {
		if (state.getState() == NetState.STATE_RUNNING) {
			for (LocalPlayerObject player:players) {
				ClientObject playerView=player.getView();
				if (playerView != null) {
					playerView.setToLocalObject(player);
					playerView.process(ticks, level);
					playerView.processSpecial(player);
				}
			}
		}
	}

	private void reInit() throws Exception {
		roundCompleted=false;
		speedMode=false;
		synchronized (world) {
			synchronized (KILL_SYNC) {
				toKill.clear();
			}
			synchronized (SYNC) {
				world.removeAllObjects();
				world.removeAllLights();
				bombMan=null;
				level=null;
				loadLevel();
				initWorld();
				initPlayers();
				initClient();
				initPlayerView();
				spawnsUsed.clear();
			}
		}
	}
}
