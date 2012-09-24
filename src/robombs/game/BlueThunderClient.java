package robombs.game;

import java.awt.Color;
import java.net.InetAddress;
import java.util.*;
import javax.swing.*;

import robombs.clientserver.*;
import robombs.game.gui.*;
import robombs.game.model.*;
import robombs.game.util.*;
import robombs.game.view.*;
import robombs.game.startup.*;
import robombs.game.sound.*;
import robombs.game.talkback.*;

import com.threed.jpct.*;
import com.threed.jpct.util.*;

/**
 * The client, i.e. the actual game. This client also opens a window with a
 * simple server browser to join an existing server or start a new one.
 */
public class BlueThunderClient extends AbstractClient implements DataTransferListener, ClientPreProcessor, GameClient, SelectionListener, EventProcessor {

	static {
		Config.glVSync = false;
		Config.glUseUnappropriateModes = true;
		Config.glTransparencyOffset = 0.0f;
		Config.maxTextures = 64;
		Config.maxPolysVisible = 25000;
		Config.collideEllipsoidThreshold = 0.075f;
		Config.collideEllipsoidSmoothing = false;
		Config.collideOffset = 150;
		Config.glTrilinear = true;
		Config.glRefresh = 0;
		Config.glZBufferDepth = 24;
		Config.glColorDepth = 24;
		Config.glWindowName = "Robombs " + Globals.GAME_VERSION;
		Config.saveMemory = true;
		Config.glAvoidTextureCopies = true;
		Config.maxAnimationSubSequences = 128;
		Config.farPlane = 500;
		Config.lightMul = 2;
		Config.glTriangleStrips = false;

		Config.glBufferedBlits = true;
		Config.glUseVBO = Globals.useVBO;
	}

	private int width = 800;
	private int height = 600;

	private boolean roundWon = false;

	private FrameBuffer buffer = null;
	private KeyMapper keyMapper = null;

	private LocalPlayerObject player = new LocalPlayerObject();
	private ClientObject playerView = null;

	private IPostProcessor blo = null;
	private KeyStates keyStates = null;

	private DecalManager decals = null;
	private ServerBrowser browser = null;
	private ServerSelection serverSel = null;
	private MouseMapper mouse = null;
	private ServerEntry selectedServer = null;

	private Highscores hiView = null;
	private HighscoreTable tmpScores = null;

	private boolean spawned = false;

	private ShadowHelper shadower = null;

	private GLFont bigFont = null;
	private GLFont bannerFont = null;
	private Table messages = null;
	private Label clickMessage = null;
	private Label roundMessage = null;

	private int cpus = Runtime.getRuntime().availableProcessors();
	private int trottleVal = cpus > 1 ? 0 : 5;
	private boolean throttling = true;

	private boolean shadows = true;
	private boolean fullScreen = false;
	private boolean shadowFilter = true;
	private int shadowSize = 4096;
	private int antiAliasing = 0;
	private int mouseSensivity = 50;

	private IconPainter icons = new IconPainter();

	private BotClient botClient = null;
	private Texture textBanner = null;
	private Texture textBannerDead = null;
	private Texture darken = null;

	private Ticker respawnTicker = new Ticker(7000);
	private Ticker tauntTicker = new Ticker(1000);
	private boolean firing = false;
	private boolean hasToRelease = false;

	private GLFont smallFont = TextBlitter.create(9);
	private long levelNameTime = 0;
	private String levelName = "";

	private boolean creditMode = true;
	private int forcedStepsBack = 0;

	private boolean mayTalkBack = TalkBack.mayTalkBack();
	private Ticker tbTimer = new Ticker(20000);
	private SkyBox skyBox = null;

	private ReflectionHelper refHelper = null;

	/**
	 * Create a new client. This starts the client and enters the game loop.
	 * 
	 * @throws Exception
	 */
	public BlueThunderClient(String[] args) throws Exception {
		if (args.length > 0) {
			if (args[0].equals("nada")) {
				shadows = false;
			}
		}
		run();
	}

	public BlueThunderClient() throws Exception {
	}

	public void selected(VideoMode vm, boolean fullScreen, int shadowQuality, boolean shadowFiltering, int aa, int mouseSpeed) throws Exception {
		Config.glSetDesiredVideoMode(vm, false);
		width = vm.width;
		height = vm.height;
		this.fullScreen = fullScreen;
		if (shadowQuality == 0) {
			shadows = false;
		} else {
			shadows = true;
			shadowSize = (int) Math.pow(2, 8 + shadowQuality);
		}
		this.shadowFilter = shadowFiltering;
		this.antiAliasing = aa;
		this.mouseSensivity = mouseSpeed;
		run();
	}

	public void connect(ServerEntry server, String name) throws Exception {
		if (!disConnectScheduled) {
			try {
				synchronized (world) {
					world.removeAllObjects();
					world.removeAllLights();
					hiView = null;
					selectedMaps.clear();
				}
				state.reset();
				NetLogger.log("Connecting...");
				while (mouse.buttonDown(0)) {
					// Avoid entering the level while the mouse button is being
					// pressed
					org.lwjgl.input.Mouse.poll();
					Thread.sleep(10);
					org.lwjgl.opengl.Display.processMessages();
				}
				shouldBeConnected = true;
				firstTransferFinished = false;
				entitiesUpdated = false;
				DataContainer dc = new DataContainer();
				dc.add(name);
				dc.add(mapList.getCheckSum());
				dc.add(0); // 0==normal Client
				dc.add(player.getObjectID());
				clientImpl = new SimpleClient(server, true, dc);
				clientImpl.addListener(this);
				clientImpl.addPreProcessor(this);
				clientImpl.connect();
				eventQueue.clear();
				selectedServer = server;
				state.setState(NetState.STATE_CONNECTED);

				// Decide if active transfers should be used (LAN) or not
				// (Internet)
				String serverIP = clientImpl.getServer();
				System.out.println(serverIP);
				if (serverIP.startsWith("127.0.0.1") || serverIP.startsWith("10.") || serverIP.startsWith("172.") || serverIP.startsWith("192.168.")) {
					Globals.activeTransfer = true;
					Globals.activeTransferForBots = true;
					NetLogger.log("Client " + getClientID() + ": LAN transfer mode selected!");
				} else {
					Globals.activeTransfer = false;
					Globals.activeTransferForBots = false;
					NetLogger.log("Client " + getClientID() + ": Internet transfer mode selected!");
				}
				/*
				 * Globals.activeTransfer=false;
				 * Globals.activeTransferForBots=false;
				 * NetLogger.log("Client "+getClientID
				 * ()+": Internet transfer mode forced!");
				 */

			} catch (Exception e) {
				if (clientImpl != null) {
					clientImpl.disconnect();
				}
				shouldBeConnected = false;
				clientImpl = null;
				if (!Config.glFullscreen) {
					JOptionPane.showMessageDialog(null, "Failed to connect: \"" + e.getMessage() + "\"");
				}
				throw e;
			}
		}
	}

	public void prepare() {
		try {
			long start = Ticker.getTime();
			state.setState(NetState.STATE_PREPARING);
			reInit(false);
			state.setState(NetState.STATE_LEVEL_LOADED);
			player.setPosition(new SimpleVector(0, Globals.skyLimit * 10f, 0));
			player.setClientID(clientImpl.getClientID());
			state.setState(NetState.STATE_WAITING);
			messages.insertCell(2, 0, "Waiting for other players!");
			Event ev = new Event(Event.LEVEL_LOADED, -99, -99, clientImpl.getClientID());
			ev.setSourceClientID(clientImpl.getClientID());
			synchronized (SYNC) {
				eventQueue.add(ev);
			}
			player.setSpecialValue(Globals.LIVES);
			waitTimer.reset();
			NetLogger.log("Level prepared in " + (Ticker.getTime() - start) + "ms.");
			levelNameTime = Ticker.getTime();

		} catch (Exception e) {
			throw new RuntimeException("Error while preparing level", e);
		}
	}

	public void begin() {
		// The Colorizer has to be resetted here, because otherwise, it may have
		// created some
		// data between a level request and the actual start of the game. This
		// is not 100% correct
		// behaviour IMHO, but it's hard to track...
		Colorizer.getInstance().init();
		player.activate();
		respawn();
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
			spawned = false;
			ready(false);
			shouldBeConnected = false;
			if (coMan != null) {
				coMan.removeAll(world);
			}
			hiView = null;
			if (player != null) {
				player.setAlive();
				player.deactivate();
			}
			synchronized (world) {
				world.removeAllObjects();
				world.removeAllLights();
			}
			roundMessage = null;
			roundCompleted = false;
		}
		if (clientImpl != null) {
			clientImpl.disconnect();
			while (clientImpl.isConnected()) {
				// Wait for the disconnect to finish...
				Thread.sleep(100);
			}
			clientImpl = null;
			if (disConnectScheduled) {
				tmpScores = null;
				disConnectScheduled = false;
				serverSel.initialAndReconnect();
			} else {
				serverSel.initial();
			}
			//
		}
		disConnectScheduled = false;
		clickMessage.setVisible(false);

		TeamAssigner.clear();

		System.runFinalization();
		System.gc();
	}

	public ServerEntry getServer() {
		return selectedServer;
	}

	public void startServer(int port) throws Exception {
		if (serverImpl == null) {
			serverImpl = new BlueThunderServer(port);
			new Thread(serverImpl).start();
		}
	}

	public void addBot() {
		if (botClient == null || !botClient.isConnected()) {
			try {
				botClient = new BotClient(serverImpl);
				botClient.run();
				while (!botClient.isConnected()) {
					Thread.sleep(50);
				}
			} catch (Exception e) {
				NetLogger.log("Error while connecting bot client to the server!");
				e.printStackTrace();
				botClient = null;
			}
		} else {
			botClient.addBot();
		}
	}

	public List<String> getPlayers() {
		if (hiView != null) {
			// Get the names from the highscore table. Quite strange...:-)
			return hiView.getPlayerNames();
		} else {
			return Collections.emptyList();
		}
	}

	public List<Integer> getTeams() {
		if (hiView != null) {
			return hiView.getTeams();
		} else {
			return Collections.emptyList();
		}
	}

	public void logout(int cid, PlayerInfo pi) {
		if (botClient != null && botClient.getClientID() == cid) {
			if (botClient.getBotCount() == 1) {
				// The last one goes away...terminate that client. That will
				// remove the remaining
				// player automatically from the server.
				serverImpl.logout(cid);
				botClient.quit();
				botClient = null;
			} else {
				botClient.removeBot(pi);
			}
		} else {
			if (serverImpl != null) {
				serverImpl.logout(cid);
			}
		}

		TeamAssigner.removeTeamAssigment(cid, pi.getObjectID());
	}

	public void shutDownServer() throws Exception {
		if (serverImpl != null) {
			if (isConnected()) {
				disconnect();
			}
			if (botClient != null) {
				// Bot client running? Terminate that sucker!
				botClient.quit();
				botClient = null;
			}
			InfoLine il = new InfoLine(InfoLine.SYSTEM_OUT, 0, "msg", "Server stopped!");
			InfoDataContainer idc = new InfoDataContainer();
			idc.add(il);
			serverImpl.broadcast(idc);
			Thread.sleep(300); // Wait to make sure that the broadcast succeeds!
			serverImpl.stop();
			serverImpl = null;
		}
	}

	public boolean runsServer() {
		return serverImpl != null;
	}

	public BlueThunderServer getServerImpl() {
		return serverImpl;
	}

	public void quit() {
		try {
			disconnect();
			shutDownServer();
		} catch (Exception e) {
			// We are on our way to exit...no time for exceptions...
		}
		quit = true;
	}

	public void beforeReceiving() {
		tmpScores = null;
		super.beforeReceiving();
	}

	public void afterReceiving() {
		if (tmpScores != null) {
			// Highscores sind mitgekommen beim Transfer!
			hiView = new Highscores(tmpScores);
			tmpScores = null;
		}
		super.afterReceiving();
	}

	public MapList getMapList() {
		return mapList;
	}

	/**
	 * Schedules a request for the next level.
	 */
	public void nextLevel() {
		if (state.getState() == NetState.STATE_RUNNING) {
			Event ev = new Event(Event.NEXT_LEVEL_REQUEST, -99, -99, clientImpl.getClientID());
			ev.setSourceClientID(clientImpl.getClientID());
			synchronized (SYNC) {
				eventQueue.add(ev);
			}
		}
	}

	/**
	 * Starts a single player game. That's a game with all maps in the rotation
	 * plus 3 bots.
	 */
	public void startSinglePlayer() {
		firstMap();
		selectedMaps.clear();
		if (serverImpl != null) {
			serverImpl.stop();
			while (serverImpl.isRunning()) {
				try {
					Thread.sleep(100);
					NetLogger.log("Waiting for server to quit!");
				} catch (Exception e) {
				}
			}
		}
		try {
			startServer(serverSel.getPort());

			while (!serverImpl.isRunning()) {
				try {
					Thread.sleep(100);
					NetLogger.log("Waiting for server to start!");
				} catch (Exception e) {
				}
			}

			ServerEntry se = new ServerEntry("Single player server", InetAddress.getLocalHost(), serverSel.getPort(), 0);
			connect(se, serverSel.getName());
			selectedMaps.addAll(mapList.getMapInfos());
			serverImpl.setSelectedMaps(selectedMaps);
			serverSel.setMaps(selectedMaps);

		} catch (Exception e) {
			NetLogger.log("Unable to start single player mode!");
			e.printStackTrace();
		}
		if (botClient != null) {
			botClient.quit();
		}
		for (int i = 0; i < 5; i++) {
			addBot();
		}
		serverImpl.setGameState(true);
		InfoDataContainer dc = new InfoDataContainer();

		int cnt = 0;
		for (MapInfo mi : selectedMaps) {
			InfoLine il = new InfoLine(InfoLine.MAP_ENTRY, cnt, mi.getName(), mi.getRealName() + "," + mi.getSet());
			dc.add(il);
			cnt++;
		}

		InfoLine il = new InfoLine(InfoLine.SERVER_STARTED_GAME, 0, "", "");
		dc.add(il);
		serverImpl.broadcast(dc);
		serverImpl.setTimeOut(20000);
		serverSel.setVisible(false);
	}

	/**
	 * Runs the client.
	 * 
	 * @throws Exception
	 */
	private void run() throws Exception {
		mapList = new MapList();
		loadTextures();
		loadSounds();
		initBuffer();
		initBrowser();
		initGUI();
		gameLoop();
		exit();
	}

	/**
	 * Processes an info message from the server.
	 * 
	 * @param il
	 *            InfoLine the info
	 */
	protected void processInfo(InfoLine il) {
		if (il.getType() == InfoLine.SERVER_STARTED_GAME && state.getState() <= NetState.STATE_WAITING) {
			serverSel.setVisible(false);
			NetLogger.log("Server has started the game!");
			messages.insertCell(2, 0, "Loading game data!");
			reloadScheduled = true;
		}

		if (il.getType() == InfoLine.NEXT_LEVEL && state.getState() == NetState.STATE_RUNNING) {
			NetLogger.log("Server switches to the next level!");
			serverSel.setVisible(false);
			messages.insertCell(2, 0, "Loading next level!");
			nextMap();
			reloadScheduled = true;
		}

		if (il.getType() == InfoLine.ALL_CLIENTS_READY && state.getState() == NetState.STATE_WAITING) {
			NetLogger.log("Client " + clientImpl.getClientID() + ": All clients are ready to go!");
			messages.insertCell(2, 0, "Starting game!");
			playerCount = Integer.parseInt(il.getValue());
			respawnCount = 0;
			begin();
		}

		if (il.getType() == InfoLine.END_GAME && state.getState() == NetState.STATE_RUNNING) {
			NetLogger.log("Server wants to end game!");
			disConnectScheduled = true;
		}

		if (il.getType() == InfoLine.ROUND_COMPLETED) {
			NetLogger.log("Round completed");
			roundCompleted = true;
			String name = il.getKey();
			int team = il.getCount();
			boolean teamWin = team != 0;
			String teamName = il.getValue();

			if (name.length() == 0) {
				name = "DRAW!";
			} else {
				if (teamWin) {
					name = teamName;
				}
				name += " has won!";
			}

			int dim = TextBlitter.getWidth(bannerFont, name) / 2;
			roundMessage = new Label((int) (buffer.getOutputWidth() / 2 - buffer.getOutputWidth() / 13.3333f - dim),
					(int) (buffer.getOutputHeight() / 2 + buffer.getOutputWidth() / 10.66666f) - bannerFont.fontHeight / 2);
			roundMessage.setFont(bannerFont);
			roundMessage.setText(name);
			roundMessage.setColor(Color.BLACK);

			if (player.isDead() && player.getSpecialValue() <= 0 && (!teamWin || TeamAssigner.getTeam(player.getClientID(), player.getObjectID()) != team)) {
				// Obviously no win
				SoundManager.getInstance().play("boo", player.getPosition());
				roundWon = false;
			} else {
				// a win!
				SoundManager.getInstance().play("happykids", player.getPosition());
				roundWon = true;
			}

			roundMessage.setVisible(true);
		}

		if (il.getType() == InfoLine.PLAYER_ADDED) {
			NetLogger.log("'" + il.getValue() + "' added!");
		}

		if (il.getType() == InfoLine.TEAM_ASSIGNED) {
			int team = il.getCount();
			int cid = Integer.parseInt(il.getKey());
			int oid = Integer.parseInt(il.getValue());
			TeamAssigner.assignTeam(cid, oid, team);
			NetLogger.log("Client " + cid + " has been assigned to team " + team);
			if (runsServer()) {
				serverImpl.sendScores();
			}
		}

		if (il.getType() == InfoLine.READY_YOURSELF && !isReady() && !runsServer()) {
			messages.insertCell(2, 0, "The server wants to start the game - please ready yourself!");
			serverSel.directMessage("The server wants to start the game - please ready yourself!");
		}

		if (il.getType() == InfoLine.PLAYER_REMOVED) {
			NetLogger.log("'" + il.getValue() + "' removed!");
			int cid = Integer.parseInt(il.getKey());
			if (cid == clientImpl.getClientID()) {
				// Somehow, a message arrived that this client's player has been
				// disconnected.
				// This can be a server-side drop, we have to process it...
				try {
					disconnect();
				} catch (Exception e) {
					NetLogger.log("Unable to disconnect - already disconnected?");
				}
			}
			respawnCount++; // Yes, this should really be ++, not --
		}
		if (il.getType() == InfoLine.SCORE_TABLE) {
			if (tmpScores == null || (tmpScores.getLineCount() > 0 && il.getKey().equals("name") && il.getCount() == 0)) {
				NetLogger.log("Client " + getClientID() + ": Receiving new high score list!");
				tmpScores = new HighscoreTable();
			}
			tmpScores.addLineEntry(il);
		}
		if (il.getType() == InfoLine.MAP_ENTRY) {
			// This logic is a bit unsafe, because it assumes, that the entries
			// arrive in the
			// correct order. However, this is always the case and the way how
			// the server works
			// ensures this, so it doesn't matter...
			if (il.getCount() == 0) {
				selectedMaps.clear();
				firstMap();
			}
			if (il.getKey().length()>0) {
				selectedMaps.add(new MapInfo(il.getKey(), il.getValue()));
			}
		}
		if (il.getType() == InfoLine.SYSTEM_OUT) {
			messages.insertCell(2, 0, il.getValue());
		}
	}

	public void sendEvent(Event event) {
		synchronized (SYNC) {
			eventQueue.add(event);
		}
	}

	/**
	 * Processes an event from the server. An event if something like
	 * "player fires a shot" or "player has died".
	 * 
	 * @param event
	 *            Event the event
	 */
	public void processEvent(Event event) {
		switch (event.getType()) {
		case Event.TAUNT:
			// event.getOrigin() ist oftmals zu leise...daher nur aus der
			// Richtung aber laut!
			SimpleVector p = event.getOrigin();
			p = p.calcSub(player.getPosition());
			p = p.normalize();
			SimpleVector s = new SimpleVector(player.getPosition());
			s.add(p);
			SoundManager.getInstance().play("taunt" + event.getValue(), s);
			break;
		case Event.PLAYER_DAMAGE:
			if (event.getTargetClientID() == clientImpl.getClientID()) {
				// Myself
				player.addToValue(event.getValue());
			}
			break;
		case Event.PLAYER_DEAD:
			if (!roundCompleted) {
				SoundManager.getInstance().play("scream", event.getOrigin());
				if (event.getTargetClientID() != clientImpl.getClientID()) {
					// Somebody else hit
					ClientObject obj = coMan.getClientObject(event.getTargetID(), event.getTargetClientID());
					if (obj != null) {
						// Can't be null anyway...
						obj.setCollisionMode(Object3D.COLLISION_CHECK_NONE);
					}
				} else {
					// Myself
					player.setDead();
					respawnTicker.reset();
					playerView.setCollisionMode(Object3D.COLLISION_CHECK_NONE);
					if (player.getSpecialValue() <= 0) {
						// All lives lost? Tell the server that we are out for
						// this round.
						Event ev = new Event(Event.ROUND_LOST, player, player);
						eventQueue.add(ev);

						SimpleVector viewPos = level.getCenter();
						viewPos.y -= 200;
						Camera cam = level.createAllViewingCamera(viewPos, buffer);
						SoundManager.getInstance().play("boo", player.getPosition());
						// processEvent is already synchronized to world...no
						// need to resync it here...
						Camera camy = world.getCamera();
						camy.setBack(cam.getBack());
						camy.setPosition(cam.getPosition());
						camy.setFOV(Math.max(cam.getFOV(), cam.getYFOV()));
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
			SoundManager.getInstance().play("splash", event.getOrigin());
			break;
		case Event.BOMB_TOUCHED:
			if (player.isInvincible() && event.getSourceClientID() == clientImpl.getClientID() && !player.getPlayerPowers().canKick()) {
				// The local player is inVincible and has touched some bomb?
				// Make it explode!
				Event eve = new Event(Event.BOMB_OVERLOADED, event.getSourceID(), event.getTargetID(), event.getTargetClientID());
				eventQueue.add(eve);
			} else {
				if (event.getTargetClientID() == clientImpl.getClientID() /*
																		 * &&
																		 * player
																		 * .
																		 * getPlayerPowers
																		 * (
																		 * ).canKick
																		 * ()
																		 */) {
					// Created by myself
					boolean kicked = bombMan.startMoving(event.getTargetID(), event);
					if (kicked) {
						forcedStepsBack = Globals.stepBacks;
					}
				}
				SoundManager.getInstance().play("woohoo", event.getOrigin());
			}
			break;
		case Event.REMOVE_CRATE:
			level.getCrateManager().explode(event, level);
			break;
		case Event.COLLECT_ITEM:
			boolean bonus = level.getItemManager().collect(event, level, event.getSourceClientID() == clientImpl.getClientID(), player);

			String sound = "bounce";
			String texture = "star";
			if (!bonus) {
				sound = "cough";
				texture = "slime";
			}
			SoundManager.getInstance().play(sound, event.getOrigin()); // Play
			// sound...

			// ...and create stars...silly idea to do this here...
			ParticleManager pm = level.getParticleManager();
			SimpleVector vel = new SimpleVector();
			int end = 10;
			if (Globals.enhancedGraphics) {
				end = 20;
			}
			for (int i = 0; i < end; i++) {
				vel.y = -0.3f - (float) Math.random() / 7f;
				vel.x = 0.5f * (float) (Math.random() - 0.5f);
				vel.z = 0.5f * (float) (Math.random() - 0.5f);
				pm.addParticle(event.getOrigin(), vel, 10f, 2000, texture, 2f + (float) Math.random(), true, false, false, Color.WHITE);
			}
			break;
		case Event.LOGIN_REJECTED:
			if (event.getTargetClientID() == clientImpl.getClientID()) {
				if (event.getSourceID() == -99) {
					serverSel.message("Login rejected - the game is already running!");
				}
				if (event.getSourceID() == -98) {
					serverSel.message("Login rejected - level data mismatch!");
				}
				NetLogger.log("Client login rejected by server!");
				try {
					disconnect();
					serverSel.setVisible(true);
				} catch (Exception e) {
					NetLogger.log("Unable to disconnect from server!");
				}
			}
			break;
		case Event.FIRE:
			if (event.getSourceClientID() != clientImpl.getClientID()) {
				// Someone else has fired!
				dropper.addDrop(event.getOrigin(), event.getDirection(), level, true);
				if (fireTicker.getTicks() > 0) {
					// This isn't really correct...but making it correct is very
					// expensive. So we don't care for now...
					SoundManager.getInstance().play("water", event.getOrigin());
					fireTicker.reset();
				}
			}
			break;
		case Event.PLACE_BOMB:
			if (event.getSourceClientID() != clientImpl.getClientID()) {
				// Someone else has placed a bomb!
				NetLogger.log("Bomb placed at " + event.getOrigin());
				GridPosition gp = bombMan.getBombMask().getGrid(event.getOrigin().x, event.getOrigin().z);
				bombMan.getBombMask().setMaskAt(gp, MapMask.BOMB);
			}
			SoundManager.getInstance().play("drop", event.getOrigin());
			break;
		case Event.ENTITY_REMOVE:
			// For "normal" Objects, the server just doesn't transmit a removed
			// object. For bombs,
			// this isn't sufficient, because they have to be removed from the
			// manager's grid mask
			// This is why the server broadcasts this removal. There is no
			// check, if this is really a bomb.
			// The server doesn't broadcast anything else ATM, so it's hacky but
			// fine this way. If this
			// changes, we need different events for some kind of typing.
			if (event.getSourceClientID() != clientImpl.getClientID()) {
				// Someone else has removed a bomb...
				ClientObject co = coMan.getClientObject(event.getSourceID(), event.getSourceClientID());
				GridPosition gp2 = null;
				if (co != null) {
					SimpleVector sv = co.getTransformedCenter();
					gp2 = bombMan.getBombMask().getGrid(sv.x, sv.z);
				}
				NetLogger.log("Bomb removed at " + event.getOrigin());
				GridPosition gp = bombMan.getBombMask().getGrid(event.getOrigin().x, event.getOrigin().z);
				if (gp2 != null && !gp.equals(gp2)) {
					// This is a little bit kludgy, but i had so many problems
					// with bombs remaining in
					// the grid that were long gone, that i want to be 110% sure
					// now...:-)
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
				// Myself...
				player.setValue(100);
				SoundManager.getInstance().play("comeon", player.getPosition());
			} else {
				ClientObject obj = coMan.getClientObject(event.getSourceID(), event.getSourceClientID());
				if (obj != null) {
					obj.setCollisionMode(Object3D.COLLISION_CHECK_OTHERS | Object3D.COLLISION_CHECK_SELF);
				}
			}
			respawnCount++;
			break;
		}
	}

	/**
	 * Initializes the startup-GUI, i.e. the server browser.
	 * 
	 * @throws Exception
	 */
	private void initGUI() throws Exception {
		messages = new Table("msgs", 3, 1, 10, height - 60, 500, height - 10);
		serverSel = new ServerSelection(browser, this, buffer.getOutputHeight());
		serverSel.setVisible(false);
		bigFont = TextBlitter.create(buffer.getOutputWidth() / 20);
		createMessageText("Click to respawn!", true);
		clickMessage.setVisible(false);
		bannerFont = TextBlitter.create(buffer.getOutputWidth() / (2 * 15));
	}

	private void createMessageText(String txt, boolean semiCenter) {
		int dim = TextBlitter.getWidth(bigFont, txt) / 2;
		int sub = 0;
		int add = 0;
		Color col = Color.BLACK;
		if (semiCenter) {
			sub = buffer.getOutputWidth() / 16;
			add = buffer.getOutputHeight() / 4;
			col = Color.WHITE;
		}
		clickMessage = new Label(buffer.getOutputWidth() / 2 - dim - sub, buffer.getOutputHeight() / 2 - bigFont.fontHeight / 2 + add);
		clickMessage.setFont(bigFont);
		clickMessage.setText(txt);
		clickMessage.setColor(col);
	}

	private void initBrowser() {
		if (browser != null) {
			browser.stopBrowser();
			while (browser.isRunning()) {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		browser = new ServerBrowser(7002);
		browser.startBrowser();
	}

	/**
	 * Initializes the client.
	 */
	private void initClient() {
		coMan = new ClientObjectManager(shadower, false);
		bombMan = new LocalBombManager(level, shadower);
		timeout = new Ticker(2000);
		keyStates = new KeyStates();
	}

	/**
	 * Prepares the data from the client that should be send to the server.
	 */
	protected void prepareClientData() {
		if (clientImpl != null) {
			if (state.getState() >= NetState.STATE_LEVEL_LOADED) {
				ExtendedDataContainer playerAndObjects = new ExtendedDataContainer();
				playerAndObjects.add(player);
				synchronized (KILL_SYNC) {
					toKill.addAll(bombMan.fillOrKill(playerAndObjects, eventQueue));
				}
				DataContainer edc = eventQueue.getEvents(clientImpl.getClientID());
				eventQueue.clear();
				clientImpl.setContainers(new DataContainer[] { playerAndObjects, edc });
			} else {
				// Events only..
				DataContainer edc = eventQueue.getEvents(clientImpl.getClientID());
				eventQueue.clear();
				clientImpl.setContainers(new DataContainer[] { new ExtendedDataContainer(), edc });
			}
		}
	}

	private void initBuffer() {
		Logger.setOnError(Logger.ON_ERROR_THROW_EXCEPTION);

		Config.glFullscreen = fullScreen;

		buffer = new FrameBuffer(width, height, this.antiAliasing * 10); // Relies
		// on
		// the
		// constants...:-)
		buffer.disableRenderer(IRenderer.RENDERER_SOFTWARE);
		buffer.enableRenderer(IRenderer.RENDERER_OPENGL);

		shadows = shadows && buffer.supports(FrameBuffer.SUPPORT_FOR_SHADOW_MAPPING);
		/*
		 * if (!buffer.isInitialized()) { // Error with this mode? Try 1024768
		 * instead...this has to be replaced by a "real" video mode selection //
		 * in a "real" game. buffer = new FrameBuffer(1024, 768,
		 * FrameBuffer.SAMPLINGMODE_NORMAL);
		 * buffer.disableRenderer(IRenderer.RENDERER_SOFTWARE);
		 * buffer.enableRenderer(IRenderer.RENDERER_OPENGL); }
		 */
		keyMapper = new KeyMapper();
		mouse = new MouseMapper(buffer);
		fireTicker = new Ticker(900);
		bombTicker = new Ticker(200);
	}

	/**
	 * Initializes the game world.
	 */
	private void initWorld() {
		dummy = new PlayerDummy(this.getClientID());
		dummy.setEventQueue(eventQueue);
		world.addObject(dummy);
		world.getCamera().setFOVLimits(0, 100);
		dummy.setCollisionMode(Object3D.COLLISION_CHECK_SELF);

		level.addToWorld(world);

		if (Globals.skyBox && skyBox == null) {
			skyBox = new SkyBox(20000);
			if (Globals.compiledObjects) {
				skyBox.compile();
			}
		}

		world.getLights().setRGBScale(Lights.RGB_SCALE_2X);
	}

	/**
	 * Initializes the player. The local player on the client is, opposed to
	 * remote players, no real 3D object but just a data representation.
	 */
	private void initPlayer() {
		dummy.setObjectID(player.getObjectID());
		player.reset();
		player.setTurnSpeed(mouseSensivity);
	}

	private void initPlayerView() {
		playerView = ClientObjectManager.createLocalPlayerObject(player, world, eventQueue);
		playerView.setCollisionMode(Object3D.COLLISION_CHECK_SELF | Object3D.COLLISION_CHECK_OTHERS);
		playerView.addCollisionListener(new ClientObjectCollisionListener(player, true));
		if (shadows) {
			shadower.addCaster(playerView);
			// shadower.addReceiver(playerView);
		}
		player.setView(playerView);
		dummy.setMaster(playerView);
	}

	/**
	 * Fires a bullet into the current view direction of the local player.
	 */
	private void fire(long ticks) {
		if (!serverSel.isVisible() && !creditMode && state.getState() == NetState.STATE_RUNNING && allPlayersHaveSpawned()) {
			PlayerPowers powers = player.getPlayerPowers();
			boolean mayFire = (firing && powers.getWater() > 0) || (!firing && powers.getWater() == powers.getMaxWater());
			int sick = powers.isSick();
			if ((KeyStates.fire || mouse.buttonDown(0) || mouse.buttonDown(1) || sick == PlayerPowers.DROP_IMMEDIATELY) && clientImpl != null && clientImpl.isConnected()) {
				if (!player.isDead() && !hasToRelease) {

					if (mouse.buttonDown(0) && mayFire) {
						fireBullet(ticks);
					}
					if (mouse.buttonDown(1) || sick == PlayerPowers.DROP_IMMEDIATELY) {
						placeBomb();
					}
				} else {
					if (clickMessage.isVisible() && player.getSpecialValue() > 0 && player.isDead()) {
						clickMessage.setVisible(false);
						respawn();
						hasToRelease = true;
					}
				}
			} else {
				hasToRelease = false;
				bombTicker.forward();
			}

			if (sick == PlayerPowers.DROP_IMMEDIATELY) {
				// In this mode, nothing has to be released...you'll drop the
				// bomb anyway...:-)
				hasToRelease = false;
			}

			if (mouse.buttonDown(0) && mayFire && !hasToRelease) {
				firing = true;
			} else {
				if (firing) {
					SoundManager.getInstance().stop("water");
				}
				firing = false;
			}
		}
	}

	private void fireBullet(long ticks) {
		player.setAnimation(Animations.FIRE);
		player.setAnimationSpeed(20);
		/*
		 * if (fireTicker.getTicks() > 0) {
		 * 
		 * LocalObject bul = bulMan.addBullet(player, world, eventQueue); Event
		 * ev = new Event(Event.FIRE, player, bul);
		 * ev.setOrigin(player.getPosition()); eventQueue.add(ev);
		 * fireTicker.reset(); }
		 */
		SimpleVector pos = player.getPosition();

		for (int i = 0; i < ticks; i++) {
			SimpleVector[] data = dropper.addDrop(player, level);
			Event ev = new Event(Event.FIRE, player, player);
			ev.setOrigin(data[0]);
			pos = data[0];
			ev.setDirection(data[1]);
			eventQueue.add(ev);
		}
		if (!firing) {
			SoundManager.getInstance().play("water", pos, true);
		} else {
			SoundManager.getInstance().move("water", pos);
		}
	}

	public int[] getLocalPlayerInfo() {
		return new int[] { this.getClientID(), this.player.getObjectID() };
	}

	private void placeBomb() {
		if (bombTicker.getTicks() > 0) {
			if (bombMan.getCount() < player.getPlayerPowers().getBombCount()) {
				// Only do this, if you are allowed to...
				LocalObject bomb = bombMan.addBomb(clientImpl, player, world, eventQueue);
				if (bomb != null) {
					Event ev = new Event(Event.PLACE_BOMB, player, bomb);
					ev.setOrigin(player.getPosition());
					eventQueue.add(ev);
				}
			}
		}
	}

	/**
	 * Respawn!
	 */
	private void respawn() {
		if (player.getSpecialValue() > 0 && !roundCompleted && !respawnRunning) {
			// Do this in another thread to make sure, that the server has send
			// its data at least once
			// before respawning.
			entitiesUpdated = false;
			respawnRunning = true;
			new Thread() {
				public void run() {
					setPriority(Thread.MAX_PRIORITY);
					spawned = false;
					SimpleVector respawn = null;
					int its = 0;
					int cnt = 0;
					try {
						while (!entitiesUpdated && cnt < 100) {
							Thread.sleep(50);
							cnt++;
						}
					} catch (Exception e) {
					}

					if (cnt == 100) {
						respawnRunning = false;
						throw new RuntimeException("Client: Unable to respawn. Some remote data is missing!");
					}

					synchronized (SYNC) {
						List<LocalPlayerObject> pis = new ArrayList<LocalPlayerObject>();
						pis.add(player);
						do {
							SimpleVector s = new SimpleVector(spawnPoints[(clientImpl.getClientID() - 1 + its) % spawnPoints.length]);
							SimpleVector ss = new SimpleVector();
							ss.y = -3;

							if ((isFree(s, level.getCrateManager(), bombMan, BlueThunderClient.this.coMan) && world.checkCollisionSpherical(s, ss, 3f).equals(ss)) || its > 15) {
								respawn = s;
							} else {
								its++;
								NetLogger.log("Client: " + s + " is blocked!");
							}
						} while (respawn == null);

						if (player.getSpecialValue() == Globals.LIVES) {
							// Only recolor if this is a new round's start.
							Colorizer.getInstance().colorize(playerView, player);
						}
						player.setPosition(respawn);
						playerView.setCollisionMode(Object3D.COLLISION_CHECK_SELF | Object3D.COLLISION_CHECK_OTHERS);
						player.setAlive();
						player.setInvincible(true);

						// Align the player, so that he looks into the level's
						// center!
						player.getRotation().setIdentity();
						player.getViewRotation().setIdentity();
						SimpleVector pos = new SimpleVector(level.getCenter());
						pos.y = player.getPosition().y;
						pos = pos.calcSub(player.getPosition());
						float yRot = -matrixToRad(pos.getRotationMatrix()).y;
						player.getRotation().rotateY(yRot);
						player.getViewRotation().rotateAxis(player.getViewRotation().getYAxis(), yRot);

						synchronized (world) {
							// Has to be synchronized with rendering, because
							// the ShadowHelper changes the
							// world's camera.
							player.alignCamera(world.getCamera(), 50);
						}

						if (serverImpl != null) {
							serverSel.serverIsPlaying();
						} else {
							serverSel.clientIsPlaying();
						}
						spawned = true;
						Event ev = new Event(Event.PLAYER_RESPAWNED, player, player);
						ev.setOrigin(respawn);
						eventQueue.add(ev);
					}
					clickMessage.setVisible(false);
					fireTicker.reset();
					bombTicker.reset();
					hasToRelease = true;
					respawnRunning = false;
				}
			}.start();
		}
	}

	/**
	 * Updates the particles.
	 * 
	 * @param ticks
	 *            long the number of ticks passed since last call
	 */
	private void updateParticles(long ticks) {
		if (state.getState() == NetState.STATE_RUNNING) {
			level.getParticleManager().move(ticks);
			level.getSmokeCloudManager().move(ticks);
			level.getDebrisManager().process(ticks);
			dropper.process(level, bombMan, coMan);
		}
	}

	/**
	 * Draw the GUI (i.e. the server selection as there is no more GUI in this
	 * example) if visible.
	 */
	private void drawGUI() {
		if (serverSel.isVisible()) {
			mouse.show();
			serverSel.evaluateInput(mouse, keyMapper);
			serverSel.draw(buffer);
		} else {
			mouse.hide();
			if (creditMode) {
				//buffer.clear();
				buffer.blit(TextureManager.getInstance().getTexture("credits"), 0, 0, buffer.getOutputWidth() / 2 - height / 2, buffer.getOutputHeight() / 2 - height / 2, 512,
						512, height, height, 60, false, null);
				buffer.blit(TextureManager.getInstance().getTexture("crazy"), 0, 0, 0, 0, 512, 512, buffer.getOutputWidth(), buffer.getOutputHeight(), 2, true, null);

				if (KeyStates.fire || mouse.buttonDown(0) || mouse.buttonDown(1) || KeyStates.exit) {
					KeyStates.exit = false;
					KeyStates.fire = false;
					creditMode = false;
					serverSel.setVisible(true);
					while (mouse.buttonDown(0)) {
						try {
							Thread.sleep(10);
							buffer.displayGLOnly();
						} catch (Exception e) {
						}
					}
				}
			}
		}
		if (KeyStates.tab && hiView != null) {
			int width = hiView.getWidth();
			int height = hiView.getHeight();
			buffer.blit(TextureManager.getInstance().getTexture("highback"), 0, 0, hiView.getX() - 8, hiView.getY() - 8, 128, 128, width + 7, height + 14, 13, false, null);
			hiView.draw(buffer);
		}
	}

	private void drawMessages() {

		messages.draw(buffer);

		if (roundMessage == null || !roundMessage.isVisible()) {
			clickMessage.draw(buffer);
		}

		if (roundMessage != null) {
			if (roundMessage.isVisible()) {
				int width = buffer.getOutputWidth();
				int height = width / 2;
				int start = buffer.getOutputHeight() / 2 - height / 2;
				Texture tex = textBanner;
				if (!roundWon) {
					tex = textBannerDead;
				}
				buffer.blit(darken, 0, 0, 0, 0, 16, 16, width, buffer.getOutputHeight(), 5, false, null);
				buffer.blit(tex, 0, 0, 0, start, 512, 256, width, height, 20, false, null);
			}
			roundMessage.draw(buffer);
		}
	}

	private void taunt() {
		int taunt = -1;
		if (KeyStates.one) {
			taunt = 0;
		}
		if (KeyStates.two) {
			taunt = 1;
		}
		if (KeyStates.three) {
			taunt = 2;
		}
		if (KeyStates.four) {
			taunt = 3;
		}
		if (KeyStates.five) {
			taunt = 4;
		}
		if (KeyStates.six) {
			taunt = 5;
		}
		if (KeyStates.seven) {
			taunt = 6;
		}
		if (KeyStates.eight) {
			taunt = 7;
		}
		if (KeyStates.nine) {
			taunt = 8;
		}
		if (KeyStates.ten) {
			taunt = 9;
		}
		if (taunt != -1 && tauntTicker.getTicks() > 0) {
			Event ev = new Event(Event.TAUNT, -99, -99, -99);
			ev.setOrigin(player.getPosition());
			ev.setValue(taunt);
			eventQueue.add(ev);
			tauntTicker.reset();
		}
	}

	/**
	 * The game loop. This is where the game...well...loops...:-)
	 * 
	 * @throws Exception
	 */
	private void gameLoop() throws Exception {
		Ticker ticker = new Ticker(20);

		blo = new com.threed.jpct.procs.BloomGLProcessor(15, 4, 1, 1);
		// ((com.threed.jpct.procs.BloomGLProcessor) blo).setCombining(false);
		buffer.addPostProcessor(blo);
		Color clearColor = new Color(134, 178, 238);

		int fps = 0;
		int lfps = 0;
		long sleepDiv = 0;
		long start = Ticker.getTime();

		Texture blood = TextureManager.getInstance().getTexture("blood");

		int xw = buffer.getOutputWidth();
		int yw = buffer.getOutputHeight();

		// Do this once to init things...
		world.renderScene(buffer);
		world.draw(buffer);

		int sleepy = (int) ((1000f / (float) Globals.frameLimit) + 0.5f);

		Thread.currentThread().setPriority(Thread.NORM_PRIORITY + 1);

		while (!quit) {
			// System.out.println(state.getState());

			checkWaitingState();

			if (disConnectScheduled) {
				try {
					disconnect();
				} catch (Exception e) {
					// Don't care...
				}
			}

			if (reloadScheduled) {
				reloadScheduled = false;
				createMessageText("Loading...", false);
				clickMessage.setVisible(true);
				buffer.clear(clearColor);
				clickMessage.draw(buffer);
				buffer.update();
				buffer.displayGLOnly();
				prepare();
				clickMessage.setVisible(false);
				tbTimer.reset();
			}

			long ft = Ticker.getTime();
			int ticks = ticker.getTicks();
			if (ticks > 0) {
				if (!serverSel.isVisible()) {
					KeyStates.poll(keyMapper);
				}
				if (state.getState() >= NetState.STATE_WAITING) {
					synchronized (SYNC) {
						updatePlayer(ticks);
						fire(ticks);
						taunt();
						updatePlayerView(ticks);
						updateRemoteEntities(ticks, false);
						updateCrates(ticks);
						level.getCrateManager().processGlows(ticks, level, player);
						bombMan.processLocalBombs(player, level, ticks);
						checkTimeout();
						updateExplosions(ticks);
						if (eventQueue != null && clientImpl != null) {
							eventQueue.processLocalEvents(this, clientImpl.getClientID());
						}
					}
					updateCamera(ticks);
					// Thread.yield();
					updateParticles(ticks);
					cleanUp();
				}
				modified = true;
				if (clientImpl != null && Globals.activeTransfer) {
					clientImpl.triggerTransfer();
				}
			} else {
				Thread.yield();
			}

			if (spawned && !disConnectScheduled) {
				// Event processing is synced to the world too now.
				// That way, the drawing doesn't have to be synchronized to SYNC
				if (!shadows) {
					buffer.clear(clearColor);
					if (skyBox != null) {
						skyBox.render(world, buffer);
					}
					synchronized (world) {
						level.shadowPolygonsVisible(false);
						world.renderScene(buffer);
					}
					world.draw(buffer);
				} else {

					synchronized (world) {
						if (refHelper != null) {
							refHelper.drawScene(world, buffer);
						}

						shadower.updateShadowMap();
						buffer.clear(clearColor);
						if (skyBox != null) {
							skyBox.render(world, buffer);
						}
						level.shadowPolygonsVisible(false);
						shadower.drawScene(false);
						level.shadowPolygonsVisible(true);
					}
					world.draw(buffer);
				}

				if (KeyStates.bloom) {
					buffer.runPostProcessors();
				}

				if (KeyStates.wireFrame) {
					world.drawWireframe(buffer, Color.WHITE);
				}

			} else {
				buffer.clear(clearColor);
			}

			cheats();

			if (clientImpl != null && !clientImpl.isConnected() && shouldBeConnected) {
				messages.insertCell(2, 0, "Disconnected from Server!");
				serverSel.setVisible(true);
				shouldBeConnected = false;
			}

			if (player != null && player.isDead() && spawned && !roundCompleted) {
				buffer.blit(blood, 0, 0, 0, 0, 512, 512, xw, yw, 17, false, null);
			}

			// if (shadower!=null) {
			// System.out.println(shadower.getReceiverCount()+"/"+shadower.getCasterCount());
			// }

			TextBlitter.blitText(smallFont, buffer, lfps + " fps", 5, buffer.getOutputHeight() - 12, Color.YELLOW);

			if (state.getState() >= NetState.STATE_RUNNING && Ticker.hasNotPassed(levelNameTime, 6000)) {
				TextBlitter.blitText(buffer, "Welcome to '" + levelName + "'", 5, 10);
			}

			if (state.getState() == NetState.STATE_RUNNING) {
				icons.paintIcons(player, buffer);
			}

			drawMessages();

			if (state.getState() != NetState.STATE_PREPARING) {
				drawGUI();
			}

			if (Ticker.hasPassed(start, 1000)) {
				start = Ticker.getTime();
				lfps = fps;
				fps = 0;
			}

			buffer.update();
			buffer.displayGLOnly();

			if (mayTalkBack && state.getState() >= NetState.STATE_WAITING && tbTimer.getTicks() > 0) {
				TalkBack.talkBack(buffer);
				mayTalkBack = false;
			}

			fps++;

			ft = Ticker.getTime() - ft;

			throttling = KeyStates.throttle;
			long sleepTime = Math.max(trottleVal, sleepy - ft) - sleepDiv;
			if (sleepTime < 0) {
				// Sanity check, but this shouldn't happen
				sleepTime = 0;
			}
			if (throttling) {
				long st = Ticker.getTime();
				Thread.sleep(sleepTime); // Gives network and other threads some
				// time to breath
				long et = Ticker.getTime();
				sleepDiv = (et - st) - sleepTime;
				if (!Globals.correctSleepTime) {
					sleepDiv = 0;
				}
			} else {
				Thread.yield();
			}

			/*
			 * if (!runsServer()) { Thread.sleep(25); }
			 */

			if (KeyStates.exit) {
				serverSel.setVisible(!serverSel.isVisible());
				KeyStates.exit = false;
			}

			if (org.lwjgl.opengl.Display.isCloseRequested()) {
				quit();
			}
		}
	}

	private void cheats() {
		if (bombMan != null) {
			if (KeyStates.lifeTime) {
				bombMan.setLifeTime(200000);
			} else {
				bombMan.setLifeTime(2000);
			}
		}

		if (KeyStates.populate) {
			KeyStates.populate = false;
			synchronized (SYNC) {
				level.populate(bombMan, clientImpl, world, eventQueue);
			}
		}

		if (KeyStates.nextLevel) {
			KeyStates.nextLevel = false;
			nextLevel();
		}

		if (KeyStates.f1) {
			NetGlobals.clientWaitTime = 30;
		}

		if (KeyStates.f2) {
			NetGlobals.clientWaitTime = 50;
		}

		if (KeyStates.f3) {
			NetGlobals.clientWaitTime = 80;
		}
	}

	/**
	 * Load resources like textures, the level etc.
	 * 
	 * @throws Exception
	 */
	private void loadTextures() throws Exception {
		TextureUtils.loadGameTextures();
		TextureManager tm = TextureManager.getInstance();
		textBanner = tm.getTexture("textbanner");
		textBannerDead = tm.getTexture("textbannerLost");
		darken = tm.getTexture("black");
	}

	private void loadSounds() {
		SoundUtils.loadSounds();
	}

	private void loadLevel() throws Exception {
		System.gc();
		System.runFinalization();
		decals = new DecalManager(world);
		MapInfo mi = selectedMaps.get(getMapNumber());
		levelName = mi.getRealName();
		Logger.log("Loading level " + mi.getName() + "...", Logger.MESSAGE);
		level = new Level("data/levels/" + mi.getName(), mi.getSet(), world, eventQueue, decals, false);
		Camera cam = world.getCamera();
		cam.setFOV(orgFOV);
		createSpawnPoints();
		setLighting();
	}

	private void setLighting() {

		world.removeAllLights();

		int col = 100;
		if (buffer.supports(FrameBuffer.SUPPORT_FOR_RGB_SCALING)) {
			world.setAmbientLight(col >> 1, col >> 1, col >> 1);
		} else {
			world.setAmbientLight(col, col, col);
		}

		Projector proj = createProjector();
		setSun(proj);

		if (shadows) {
			enableShadowing(proj);

			if (coMan != null) {
				coMan.setShadowHelper(shadower);
			}

			if (bombMan != null) {
				bombMan.setShadowHelper(shadower);
			}

			level.makeReceiver(shadower);
			level.getCrateManager().makeCaster(shadower);
		}

		if (Globals.enhancedGraphics && Globals.reflections) {
			refHelper = level.enableReflections(buffer);
		}

	}

	private void enableShadowing(Projector proj) {
		if (shadows) {
			shadower = new ShadowHelper(world, buffer, new Projector(), shadowSize);
			shadower.setCullingMode(Globals.shadowCulling);
			shadower.setLightSource(proj);
			if (Config.glShadowZBias == 0.5f) {
				Config.glShadowZBias = 0;
			}
			shadower.setFiltering(shadowFilter);
			shadower.setAmbientLight(new Color(45, 45, 45));
		}
	}

	private Projector createProjector() {
		SimpleVector viewPos = level.getCenter();
		viewPos.y -= 200;
		viewPos.z -= 100;
		Camera cam = level.createAllViewingCamera(viewPos, buffer);
		Projector proj = new Projector();
		proj.setFOVLimits(0, 100);
		proj.setPosition(cam.getPosition());
		proj.setBack(cam.getBack());
		proj.setFOV(cam.getFOV());
		proj.setYFOV(cam.getYFOV());
		return proj;
	}

	private void setSun(Projector proj) {
		Light light = new Light(world);
		light.setPosition(proj.getPosition());
		light.setAttenuation(-1);
		light.setDiscardDistance(-1);

		int col = 100;
		if (Globals.compiledObjects) {
			col = 200;
		}

		light.setIntensity(new SimpleVector(col, col, col));
	}

	/**
	 * Terminate everything including the VM.
	 * 
	 * @throws Exception
	 */
	private void exit() throws Exception {
		if (clientImpl != null) {
			clientImpl.disconnect();
			while (clientImpl.isConnected()) {
				Thread.sleep(20);
			}
		}
		SoundManager.getInstance().dispose();
		Thread.sleep(200);
		// buffer.disableRenderer(IRenderer.RENDERER_OPENGL); // Causes a crash
		// on some machines...
		buffer.dispose();
		System.out.println("Game terminated!");
		Thread.sleep(300);
		System.exit(0);
	}

	/**
	 * Updates the local player. Albeit the local player is no mesh, this method
	 * takes care of setting the correct animation because that will be
	 * transfered to the server and all other clients.
	 * 
	 * @param ticks
	 *            long the number of ticks passed
	 */
	private void updatePlayer(long ticks) {
		if (state.getState() == NetState.STATE_RUNNING && allPlayersHaveSpawned()) {

			// System.out.println("Player at :"+level.getMask().getGrid(player.getPosition().x,
			// player.getPosition().z));

			coMan.enterCollisionMode();
			bombMan.enterCollisionMode(player);
			bombMan.moveBombs(world, ticks);

			if (!player.isDead()) {
				if (!firing) {
					player.getPlayerPowers().addWater(ticks);
				} else {
					if (!hasToRelease) {
						player.getPlayerPowers().useWater(ticks << 1);
					}
				}
				playerView.setVisibility(false);
				boolean moved = player.move(world, keyStates, mouse, ticks, dummy, forcedStepsBack);
				if (forcedStepsBack > 0) {
					forcedStepsBack -= ticks;
				}
				playerView.setVisibility(true);
				if (moved) {
					if (forcedStepsBack > 0) {
						player.setAnimation(Animations.HIT);
						player.setAnimationSpeed(30);
					} else {
						if (mouse.buttonDown(1)) {
							player.setAnimation(Animations.CROUCH_MOVE);
							player.setAnimationSpeed(20);
						} else {
							player.setAnimation(Animations.MOVE);
							player.setAnimationSpeed(40);
						}
					}
				} else {
					if (mouse.buttonDown(1)) {
						player.setAnimation(Animations.CROUCH_NONE);
					} else {
						player.setAnimation(Animations.NONE);
					}
					player.setAnimationSpeed(300);
				}
			} else {
				if (Ticker.hasPassed(player.deadSince(), 1000)) {
					player.setAnimation(Animations.DEAD);
					if (!clickMessage.isVisible()) {
						if (Ticker.hasPassed(player.deadSince(), 2000) && player.getSpecialValue() > 0 && !roundCompleted) {
							createMessageText("Click to respawn!", true);
							clickMessage.setVisible(true);
						} else {
							if (player.getSpecialValue() <= 0) {
								createMessageText("You are DEAD!", true);
								clickMessage.setVisible(true);
							}
						}
					}
				} else {
					player.setAnimation(Animations.DIE);
				}
				player.setAnimationSpeed(80);
			}

			coMan.leaveCollisionMode();
			bombMan.leaveCollisionMode();

			if (clickMessage.isVisible() && player.getSpecialValue() > 0 && respawnTicker.getTicks() > 0 && player.isDead()) {
				clickMessage.setVisible(false);
				respawn();
			}
		}
	}

	private void updatePlayerView(long ticks) {
		if (playerView != null && state.getState() == NetState.STATE_RUNNING) {
			playerView.setToLocalObject(player);
			playerView.process(ticks, level);
			playerView.processSpecial(player);
		}
	}

	private void updateCamera(int ticks) {
		if (player != null) {
			Camera camera = world.getCamera();
			player.alignCamera(camera, ticks);
		}
	}

	private void reInit(boolean withBrowser) throws Exception {
		roundCompleted = false;
		synchronized (world) {
			roundMessage = null;
			synchronized (KILL_SYNC) {
				toKill.clear();
			}
			synchronized (SYNC) {
				spawned = false;
				world.removeAllObjects();
				// Don't change the unload order to be nice to the
				// TextureManager
				// One has to know how it works internaly to optimize this, but
				// i do...:-)
				if (coMan != null) {
					// Unload player textures!
					Colorizer.getInstance().unload(buffer);
				}
				if (shadower != null) {
					shadower.dispose();
					shadower = null;
				}

				if (refHelper != null) {
					TextureManager.getInstance().unloadTexture(buffer, refHelper.getReflectionTexture());
					refHelper = null;
				}

				world.removeAllLights();
				bombMan = null;
				level = null;
				loadLevel();
				initWorld();
				initPlayer();
				if (withBrowser) {
					initBrowser();
				}
				initClient();
				initPlayerView();
			}
			// System.gc();
		}
	}

}
