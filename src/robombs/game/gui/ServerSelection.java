package robombs.game.gui;

import com.threed.jpct.*;
import com.threed.jpct.util.*;
import java.util.*;
import java.net.*;

import robombs.clientserver.*;
import robombs.game.*;
import robombs.game.util.*;
import robombs.game.sound.*;
import robombs.game.model.*;

/**
 * Sets up and manages the server selection window which is displayed after
 * startup and when pressing ESC while playing.
 */
public class ServerSelection implements DataChangeListener, GUIListener {

	public final static int GUI_STATE_INITIAL = 0;
	public final static int GUI_STATE_SERVER_STARTED = 1;
	public final static int GUI_STATE_CONNECTED = 2;
	public final static int GUI_STATE_CLIENT_PLAYING = 3;
	public final static int GUI_STATE_SERVER_PLAYING = 4;

	private final static String PATH_PREFIX = "data/levels/";

	private int playerListStart = 43;

	private List<ServerEntry> servers = null;
	private Texture backDrop = null;
	private Texture serverStarted = null;
	private Texture serverPlaying = null;
	private Texture clientStarted = null;
	private Texture clientPlaying = null;
	private ServerEntry current = null;

	private Window window = null;
	private TextField field = null;
	private TextField portField = null;
	private Button startServer = null;
	private Button connect = null;
	private Button minimize = null;
	private Button close = null;
	private Button ready = null;
	private Button addBot = null;
	private Button singlePlayer = null;
	private Button setTeam = null;
	private Label stateLabel = null;
	private Label portLabel = null;
	private Label connectLabel = null;
	private Label nameLabel = null;
	private TextField name = null;
	private Table tableLabels = null;
	private Table serverTable = null;

	// Stuff for the "real" ServerGUI
	private Table playerList = null;
	private Table mapRotation = null;
	private Table teamAssignment = null;

	private Button addToList = null;
	private Button lastlevel = null;
	private Button nextLevel = null;
	private Button addAll = null;
	private Label teamListTxt = null;
	private Label mapRotationTxt = null;

	private Image previewImg = null;
	private Image logoImg = null;

	private String state = "State: Looking for servers!";
	private GameClient client = null;
	private int guiState = GUI_STATE_INITIAL;

	private int mapPos = 0;
	private MapList mapList = null;
	private List<MapInfo> selectedMaps = new ArrayList<MapInfo>();
	private String clientMsg = null;

	private int lastPlayerCount = 0;

	/**
	 * Creates a new instance using a given server browser and client instance.
	 * Albeit this behaves like a GUIComponent in some ways, it isn't any.
	 * 
	 * @param sb
	 *            the server browser as an instance of ServerBrowser
	 * @param client
	 *            the client as an instance of GameClient
	 * @throws Exception
	 */
	public ServerSelection(ServerBrowser sb, GameClient client, int height) throws Exception {
		sb.addListener(this);
		SimpleStream ss = new SimpleStream("data/window.gif");
		backDrop = new Texture(ss.getStream());
		ss.close();

		ss = new SimpleStream("data/window_server.gif");
		serverStarted = new Texture(ss.getStream());
		ss.close();

		ss = new SimpleStream("data/window_client.gif");
		clientStarted = new Texture(ss.getStream());
		ss.close();

		ss = new SimpleStream("data/window_client_running.gif");
		clientPlaying = new Texture(ss.getStream());
		ss.close();

		ss = new SimpleStream("data/window_server_running.gif");
		serverPlaying = new Texture(ss.getStream());
		ss.close();

		mapList = client.getMapList();

		if (backDrop.getHeight() + 50 <= height) {
			window = new Window(backDrop, 50, 50);
		} else {
			window = new Window(backDrop, 50, 0);
		}
		window.setText("Version: " + Globals.GAME_VERSION);

		field = new TextField(204, 388, 272, 16);

		portField = new TextField(204, 437, 108, 16);
		portField.setText(SimpleServer.DEFAULT_PORT + "");

		name = new TextField(204, 409, 125, 16);
		String names = System.getProperty("user.name");
		if (names == null) {
			names = "Player";
		}
		name.setText(names);
		nameLabel = new Label(108, 409);
		nameLabel.setText("Player name:");

		startServer = new Button(338, 436, 138, 19);
		startServer.setLabel("Start server");

		connect = new Button(338, 409, 138, 19);
		connect.setLabel("Connect");

		ready = new Button(185, 409, 138, 19);
		ready.setLabel("Ready");
		ready.setVisible(false);

		close = new Button(477, 3, 18, 11);
		close.setHideLabel(true);
		close.setLabel("close");

		minimize = new Button(454, 3, 18, 11);
		minimize.setHideLabel(true);
		minimize.setLabel("minimize");

		close.setListener(this);
		minimize.setListener(this);
		startServer.setListener(this);
		connect.setListener(this);
		ready.setListener(this);

		stateLabel = new Label(24, 470);
		stateLabel.setText(state);

		portLabel = new Label(112, 436);
		portLabel.setText("Server port:");

		connectLabel = new Label(116, 388);
		connectLabel.setText("Connect to:");

		tableLabels = new Table("Labels", 1, 3, 24, 22, 492, 38);
		tableLabels.setColumnSize(0, 176);
		tableLabels.setColumnSize(1, 231);
		tableLabels.setColumnSize(2, 60);
		tableLabels.setRowSize(0, 16);
		tableLabels.setCell(0, 0, "Server name");
		tableLabels.setCell(0, 1, "IP/Port");
		tableLabels.setCell(0, 2, "Players");

		serverTable = new Table("Servers", 17, 3, 24, 40, 492, 325);
		serverTable.setColumnSize(0, 176);
		serverTable.setColumnSize(1, 231);
		serverTable.setColumnSize(2, 60);
		serverTable.setListener(this);

		window.add(serverTable);
		window.add(tableLabels);
		window.add(portLabel);
		window.add(connectLabel);
		window.add(stateLabel);
		window.add(field);
		window.add(portField);
		window.add(connect);
		window.add(startServer);
		window.add(close);
		window.add(minimize);
		window.add(name);
		window.add(nameLabel);
		window.add(ready);

		teamAssignment = new Table("Team", 8, 1, 32, 232, 39, 352);
		teamAssignment.setVisible(false);
		teamAssignment.setListener(this);
		window.add(teamAssignment);

		// Stuff for the "real" ServerGUI
		playerList = new Table("Players", 8, 1, playerListStart + 2, 232, 270, 352);
		playerList.setVisible(false);
		playerList.setListener(this);
		window.add(playerList);

		mapRotation = new Table("Maps", 17, 1, 285, 59, 477, 352);
		mapRotation.setListener(this);
		mapRotation.setVisible(false);
		window.add(mapRotation);

		addToList = new Button(237, 87, 33, 15);
		addToList.setLabel("Add");
		addToList.setVisible(false);
		addToList.setListener(this);
		window.add(addToList);

		addBot = new Button(155, 186, 70, 15);
		addBot.setLabel("Add bot");
		addBot.setVisible(false);
		addBot.setListener(this);
		window.add(addBot);

		lastlevel = new Button(30, 163, 27, 15);
		lastlevel.setLabel("<<");
		lastlevel.setVisible(false);
		lastlevel.setListener(this);
		window.add(lastlevel);

		nextLevel = new Button(74, 163, 27, 15);
		nextLevel.setLabel(">>");
		nextLevel.setVisible(false);
		nextLevel.setListener(this);
		window.add(nextLevel);

		addAll = new Button(155, 163, 70, 15);
		addAll.setLabel("Add all");
		addAll.setVisible(false);
		addAll.setListener(this);
		window.add(addAll);

		singlePlayer = new Button(338, 340, 138, 15);
		singlePlayer.setLabel("Single player");
		singlePlayer.setListener(this);
		window.add(singlePlayer);

		setTeam = new Button(155, 209, 70, 15);
		setTeam.setLabel("No team");
		setTeam.setListener(this);
		setTeam.setVisible(false);
		window.add(setTeam);

		teamListTxt = new Label(30, 212);
		teamListTxt.setText("Team / Player");
		teamListTxt.setVisible(false);
		window.add(teamListTxt);

		mapRotationTxt = new Label(282, 38);
		mapRotationTxt.setText("Map rotation");
		mapRotationTxt.setVisible(false);
		window.add(mapRotationTxt);

		previewImg = new Image(32, 30, 191, 117, null);
		previewImg.setVisible(false);
		window.add(previewImg);

		logoImg = new Image(32, 30, 200, 160, null);
		logoImg.setVisible(false);
		logoImg.setImage("data/logo.jpg");
		window.add(logoImg);

		this.client = client;

		setCurrentImage();

	}

	private void setCurrentImage() {
		if (mapPos < mapList.getMapInfos().size() && mapPos > -1) {
			previewImg.setImage(PATH_PREFIX + mapList.getMapInfos().get(mapPos).getPicture());
		}
	}

	@SuppressWarnings(value = "unchecked")
	public void dataChanged(Object data) {
		if (data instanceof List) {

			servers = (List<ServerEntry>) data;
			Collections.sort(servers);
			serverTable.clear();

			if (servers != null && servers.size() > 0) {
				int cnt = 0;
				for (ServerEntry se : servers) {
					serverTable.setCell(cnt, 0, se.getName());
					serverTable.setCell(cnt, 1, se.getAddress().getHostAddress() + ":" + se.getPort());
					serverTable.setCell(cnt, 2, Integer.valueOf(se.getClientCount()));
					cnt++;
				}
			}

		} else {
			throw new RuntimeException("What the hell...???");
		}
	}

	/**
	 * Enables/Disables the whole ServerSelection.
	 * 
	 * @param visible
	 *            visible...or not...
	 */
	public void setVisible(boolean visible) {
		window.setVisible(visible);
		if (clientMsg == null) {
			if (client.isConnected()) {
				stateLabel.setText("State: Connected!");
			} else {
				stateLabel.setText("State: Looking for servers!");
			}
		} else {
			stateLabel.setText(clientMsg);
		}
		if (visible) {
			SoundManager.getInstance().setListener(SimpleVector.ORIGIN, 0);
		}
	}

	/**
	 * Is the ServerSelection visible?
	 * 
	 * @return boolean is it?
	 */
	public boolean isVisible() {
		return window.isVisible();
	}

	public void setGUIState(int state) {
		if (guiState != state) {
			guiState = state;
			switch (guiState) {
			case (GUI_STATE_INITIAL):
				lastPlayerCount = 0;
				setSharedGUIElements(true);
				ready.setVisible(false);
				setServerGUIElements(false);
				logoImg.setVisible(false);
				window.setWindowTexture(backDrop);
				connect.setLabel("Connect");
				singlePlayer.setVisible(true);
				setTeam.setVisible(false);
				break;
			case (GUI_STATE_SERVER_STARTED):
				clientMsg = null;
				setSharedGUIElements(false);
				startServer.setVisible(true);
				setServerGUIElements(true);
				logoImg.setVisible(false);
				window.setWindowTexture(serverStarted);
				connect.setLabel("Waiting...");
				singlePlayer.setVisible(false);
				setTeam.setVisible(true);
				break;
			case (GUI_STATE_CONNECTED):
				clientMsg = null;
				setSharedGUIElements(false);
				ready.setVisible(true);
				setServerGUIElements(false);

				playerList.setVisible(true);
				teamAssignment.setVisible(true);
				mapRotation.setVisible(true);
				teamListTxt.setVisible(true);
				mapRotationTxt.setVisible(true);
				logoImg.setVisible(true);
				window.setWindowTexture(clientStarted);
				connect.setLabel("Connect");
				ready.setLabel("Ready");
				singlePlayer.setVisible(false);
				setTeam.setVisible(true);
				break;
			case (GUI_STATE_CLIENT_PLAYING):
				clientMsg = null;
				setSharedGUIElements(false);
				setServerGUIElements(false);

				playerList.setVisible(true);
				mapRotation.setVisible(true);
				teamListTxt.setVisible(true);
				mapRotationTxt.setVisible(true);
				logoImg.setVisible(true);
				window.setWindowTexture(clientPlaying);
				connect.setLabel("Disconnect");
				singlePlayer.setVisible(false);
				setTeam.setVisible(false);
				break;
			case (GUI_STATE_SERVER_PLAYING):
				clientMsg = null;
				setSharedGUIElements(false);
				startServer.setVisible(true);
				setServerGUIElements(true);

				addBot.setVisible(false);
				addToList.setVisible(false);
				lastlevel.setVisible(false);
				nextLevel.setVisible(false);
				addAll.setVisible(false);
				previewImg.setVisible(false);
				logoImg.setVisible(true);
				window.setWindowTexture(serverPlaying);
				connect.setLabel("End game");
				singlePlayer.setVisible(false);
				setTeam.setVisible(false);
				break;
			default:
				throw new RuntimeException("Unknown GUI state: " + guiState);
			}
		}
	}

	/**
	 * Processes key and mouse events. What this actually does, is to redirect
	 * such events to the server selection window, which is a real GUIComponent
	 * which can process these events.
	 * 
	 * @param mouse
	 *            the MouseMapper
	 * @param keyMapper
	 *            the KeyMapper
	 */
	public void evaluateInput(MouseMapper mouse, KeyMapper keyMapper) {
		boolean something = window.evaluateInput(mouse, keyMapper);

		if (!something && window.hasKeyEvent(java.awt.event.KeyEvent.VK_ESCAPE)) {
			setVisible(false);
			KeyStates.exit = false;
		}
	}

	/**
	 * Draws the ServerSelection.
	 * 
	 * @param buffer
	 *            the FrameBuffer to draw to.
	 */
	public void draw(FrameBuffer buffer) {
		if (!client.runsServer()) {
			if (client.isConnected()) {
				connect.setLabel("Disconnect");
			} else {
				connect.setLabel("Connect");
				if (guiState != GUI_STATE_INITIAL) {
					setGUIState(GUI_STATE_INITIAL);
				}
			}
		} else {
			if (client.isConnected()) {
				BlueThunderServer server = client.getServerImpl();
				if (server.getStateCount(NetState.STATE_READY) == server.getClientCount() - 1) {
					// All ready excluding myself...
					if (selectedMaps.size() != 0 && allTeamsAssigned()) {
						connect.setLabel("Start game");
					} else {
						connect.setLabel("Waiting..."); // @todo: Better naming!
					}
				} else {
					connect.setLabel("Waiting (" + (server.getClientCount() - server.getStateCount(NetState.STATE_READY) - 1) + ")...");
				}
				if (server.getGameState()) {
					connect.setLabel("End game");
				}
			}
		}

		if (guiState != GUI_STATE_INITIAL) {
			int[] inf = client.getLocalPlayerInfo();
			int cid = inf[0];
			int oid = inf[1];
			int team = TeamAssigner.getTeam(cid, oid);
			if (team == 0) {
				setTeam.setLabel("No team");
			} else {
				setTeam.setLabel("Team: " + team);
			}
		}

		if (client.isReady()) {
			ready.setLabel("Unready");
		} else {
			ready.setLabel("Ready");
		}

		if (client.runsServer()) {
			startServer.setLabel("Stop server");
		} else {
			startServer.setLabel("Start server");
		}

		fillPlayerAndTeamList();
		fillMapRotation();

		window.draw(buffer);
	}

	private boolean allTeamsAssigned() {
		if (client.runsServer()) {
			boolean oneRealTeam = false;
			boolean oneUnassigned = false;
			List<PlayerInfo> players = client.getServerImpl().getPlayers();
			for (PlayerInfo pi : players) {
				int team = TeamAssigner.getTeam(pi);
				if (team == 0) {
					oneUnassigned = true;
				} else {
					oneRealTeam = true;
				}
				if (oneRealTeam && oneUnassigned) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	public void clientIsPlaying() {
		setVisible(false);
		setGUIState(ServerSelection.GUI_STATE_CLIENT_PLAYING);
	}

	public void serverIsPlaying() {
		setVisible(false);
		setGUIState(ServerSelection.GUI_STATE_SERVER_PLAYING);
	}

	public void sendMapsToAll() {
		InfoDataContainer dc = new InfoDataContainer();
		sendMaps(dc);
		client.getServerImpl().broadcast(dc);
	}

	private void waitForDisconnect() {
		while (client.isConnected()) {
			try {
				Thread.sleep(20);
			} catch (Exception e) {
				// Don't care
			}
		}
		if (client.runsServer()) {
			// Just to be sure...
			client.getServerImpl().setGameState(false);
		}
	}

	public void initial() {
		waitForDisconnect();
		setGUIState(ServerSelection.GUI_STATE_INITIAL);
		setVisible(true);
	}

	public void initialAndReconnect() {

		waitForDisconnect();
		if (!client.runsServer()) {
			try {
				// Give the server some time to recover.
				Thread.sleep(100);
			} catch (Exception e) {
				// Don't care
			}
		}
		connect(field.getText());
		if (client.isConnected()) {
			if (client.runsServer()) {
				setGUIState(ServerSelection.GUI_STATE_SERVER_STARTED);
			} else {
				setGUIState(ServerSelection.GUI_STATE_CONNECTED);
			}
		} else {
			setGUIState(ServerSelection.GUI_STATE_INITIAL);
			stateLabel.setText("State: Connection failed!");
		}
		setVisible(true);
	}

	public void connect() {
		String url = field.getText();
		if ((url == null || url.length() == 0) && servers != null && servers.size() > 0) {
			ServerEntry se = servers.get(0);
			url = se.getAddress().getHostAddress() + ":" + se.getPort();
		}
		connect(url);
		if (client.isConnected()) {
			setGUIState(GUI_STATE_CONNECTED);
		} else {
			setGUIState(GUI_STATE_INITIAL);
		}
	}

	public void message(String msg) {
		clientMsg = msg;
	}

	public void directMessage(String msg) {
		clientMsg = msg;
		stateLabel.setText(msg);
	}

	public void elementChanged(String label, String data) {

		// System.out.println(label);

		SoundManager.getInstance().play("click", SimpleVector.ORIGIN);

		if (label.equals("<<")) {
			mapPos--;
			if (mapPos == -1) {
				mapPos = mapList.getMapInfos().size() - 1;
			}
			setCurrentImage();
		}

		if (label.equals(">>")) {
			mapPos++;
			if (mapPos == mapList.getMapInfos().size()) {
				mapPos = 0;
			}
			setCurrentImage();
		}

		if (label.equalsIgnoreCase("add")) {
			if (selectedMaps.size() < 18) {
				selectedMaps.add(mapList.getMapInfos().get(mapPos));
				sendMapsToAll();
			}
		}

		if (label.equalsIgnoreCase("add all")) {
			selectedMaps.clear();
			selectedMaps.addAll(mapList.getMapInfos());
			sendMapsToAll();
		}

		if (label.equalsIgnoreCase("add bot")) {
			client.addBot();
		}

		if (label.equalsIgnoreCase("Maps") && guiState == GUI_STATE_SERVER_STARTED) {
			int pos = new Integer(data).intValue();
			if (selectedMaps != null && selectedMaps.size() > pos) {
				selectedMaps.remove(pos);
				sendMapsToAll();
			}
		}

		if (label.equalsIgnoreCase("Players") && guiState == GUI_STATE_SERVER_STARTED) {
			int pos = new Integer(data).intValue();
			List<PlayerInfo> players = client.getServerImpl().getPlayers();
			if (pos < players.size()) {
				PlayerInfo pi = players.get(pos);
				int cid = pi.getClientID();
				if (cid != client.getClientID()) {
					client.logout(cid, pi);
				}
			}
		}

		if (label.equalsIgnoreCase("Team") && guiState == GUI_STATE_SERVER_STARTED) {
			int pos = new Integer(data).intValue();
			List<PlayerInfo> players = client.getServerImpl().getPlayers();
			if (pos < players.size()) {
				PlayerInfo pi = players.get(pos);
				int cid = pi.getClientID();
				int oid = pi.getObjectID();
				int team = TeamAssigner.rotateTeam(cid, oid);

				BlueThunderServer server = client.getServerImpl();
				InfoDataContainer idc = new InfoDataContainer();
				InfoLine il = new InfoLine(InfoLine.TEAM_ASSIGNED, team, String.valueOf(cid), String.valueOf(oid));
				idc.add(il);
				server.broadcast(idc);
			}
		}

		if (label.equalsIgnoreCase("connect")) {
			connect();
		}

		if (label.equalsIgnoreCase("End game")) {
			BlueThunderServer server = client.getServerImpl();
			InfoDataContainer idc = new InfoDataContainer();
			InfoLine il = new InfoLine(InfoLine.END_GAME, 0, "", "");
			client.getServerImpl().setGameState(false);
			idc.add(il);
			server.broadcast(idc);
		}

		if (label.equalsIgnoreCase("ready")) {
			client.ready(true);
			clientMsg = null;
			setVisible(true);
		}

		if (label.equalsIgnoreCase("unready")) {
			client.ready(false);
		}

		if (label.equalsIgnoreCase("No team") || label.startsWith("Team: ")) {
			int[] inf = client.getLocalPlayerInfo();
			int cid = inf[0];
			int oid = inf[1];
			int team = TeamAssigner.rotateTeam(cid, oid);
			if (team == 0) {
				setTeam.setLabel("No team");
			} else {
				setTeam.setLabel("Team: " + team);
			}

			Event eve = new Event(Event.TEAM_SET, 0, 0, 0);
			SimpleVector vars = new SimpleVector(team, cid, oid);
			eve.setOrigin(vars);
			client.sendEvent(eve);
		}

		if (label.equalsIgnoreCase("Start game") && client.runsServer()) {
			if (selectedMaps.size() > 0 && allTeamsAssigned()) {
				BlueThunderServer server = client.getServerImpl();
				server.setGameState(true);
				InfoDataContainer dc = new InfoDataContainer();

				sendMaps(dc);
				client.getServerImpl().setSelectedMaps(selectedMaps);

				// Retransmit all assignments!
				List<int[]> assignments = TeamAssigner.getAssignments();
				for (int[] part : assignments) {
					InfoLine il = new InfoLine(InfoLine.TEAM_ASSIGNED, part[2], String.valueOf(part[0]), String.valueOf(part[1]));
					dc.add(il);
				}

				InfoLine il = new InfoLine(InfoLine.SERVER_STARTED_GAME, 0, "", "");
				dc.add(il);
				server.broadcast(dc);
				server.setTimeOut(20000);
				setVisible(false);
			}
		}

		if (label.equalsIgnoreCase("Single player")) {
			client.startSinglePlayer();
		}

		if (label.equalsIgnoreCase("waiting...")) {
			if (selectedMaps.size() == 0) {
				directMessage("State: Please select at least one map!");
			} else {
				if (!allTeamsAssigned()) {
					directMessage("State: Please assign a team to each player!");
				}
			}
		}

		if (label.startsWith("Waiting (")) {
			InfoDataContainer dc = new InfoDataContainer();
			InfoLine il = new InfoLine(InfoLine.READY_YOURSELF, 0, "", "");
			dc.add(il);
			client.getServerImpl().broadcast(dc);
		}

		// Disconnect
		if (label.equalsIgnoreCase("disconnect")) {
			if (client.isConnected()) {
				try {
					client.disconnect();
					stateLabel.setText("State: Looking for servers!");
					setGUIState(GUI_STATE_INITIAL);
				} catch (Exception e) {
					stateLabel.setText("State: Can't disconnect from that server!");
					e.printStackTrace();
					return;
				}
			}
		}

		// Starting server
		if (label.equalsIgnoreCase("Start server")) {
			if (!client.runsServer()) {
				int port = getPort();

				try {
					client.startServer(port);

					int cnt = 0;
					while (!client.getServerImpl().isRunning() && cnt <= 50) {
						Thread.sleep(100);
						cnt++;
					}

					if (!client.getServerImpl().isRunning()) {
						client.shutDownServer();
						throw new RuntimeException("Unable to start server within 5 seconds!");
					}

					stateLabel.setText("State: Local server started!");
					connect(portField.getText());
					if (client.isConnected()) {
						setGUIState(GUI_STATE_SERVER_STARTED);
					} else {
						stateLabel.setText("State: Can't start local server - try another port!");
						setGUIState(GUI_STATE_INITIAL);
						client.shutDownServer();
					}
				} catch (Exception e) {
					stateLabel.setText("State: Can't start local server!");
					setGUIState(GUI_STATE_INITIAL);
					e.printStackTrace();
					return;
				}
			}
		}

		// Stoping Server
		if (label.equalsIgnoreCase("Stop server")) {
			if (client.runsServer()) {
				try {
					client.getServerImpl().setGameState(false);
					client.shutDownServer();
					stateLabel.setText("State: Local server stopped!");
					setGUIState(GUI_STATE_INITIAL);
				} catch (Exception e) {
					stateLabel.setText("State: Can't stop local server!");
					e.printStackTrace();
					return;
				}
			}
		}

		// Server selected
		if (label.equalsIgnoreCase("Servers")) {
			int pos = new Integer(data).intValue();
			if (servers != null && servers.size() > pos) {
				ServerEntry se = (ServerEntry) servers.get(pos);
				field.setText(se.getAddress().getHostAddress() + ":" + se.getPort());
			}
		}

		if (label.equalsIgnoreCase("close")) {
			client.quit();
		}

		if (label.equalsIgnoreCase("minimize")) {
			setVisible(false);
		}
	}

	public int getPort() {
		int port = SimpleServer.DEFAULT_PORT;
		try {
			port = Integer.parseInt(portField.getText());
		} catch (Exception e) {
		}
		return port;
	}

	public String getName() {
		return name.getText();
	}

	public void setMaps(List<MapInfo> maps) {
		selectedMaps.clear();
		selectedMaps.addAll(maps);
		fillMapRotation();
	}

	private void sendMaps(InfoDataContainer dc) {
		int cnt = 0;
		for (MapInfo mi : selectedMaps) {
			InfoLine il = new InfoLine(InfoLine.MAP_ENTRY, cnt, mi.getName(), mi.getRealName() + "," + mi.getSet());
			dc.add(il);
			cnt++;
		}
		if (selectedMaps.size() == 0) {
			InfoLine il = new InfoLine(InfoLine.MAP_ENTRY, 0, "", "");
			dc.add(il);
		}
	}

	private void connect(String data) {
		int pos = data.indexOf(":");
		int port = SimpleServer.DEFAULT_PORT;
		String host = "127.0.0.1";

		if (pos != -1) {
			try {
				port = Integer.parseInt(data.substring(pos + 1));
				host = data.substring(0, pos);
			} catch (Exception e) {
			}
			data = data.substring(0, pos);
		} else {
			try {
				port = Integer.parseInt(data);
			} catch (Exception e) {
			}
		}
		try {
			current = new ServerEntry("unknown", InetAddress.getByName(host), port, 0);
			client.connect(current, name.getText());
			stateLabel.setText("State: Connected!");
		} catch (Exception e) {
			setVisible(true);
			stateLabel.setText("State: Can't connect to that server!");
			e.printStackTrace();
			// throw new RuntimeException(e);
		}
	}

	private void fillMapRotation() {
		if (this.isVisible() && this.mapRotation.isVisible()) {
			mapRotation.clear();
			int cnt = 0;
			List<MapInfo> maps = selectedMaps;
			if (!client.runsServer()) {
				// No server, so get the maps from the client...
				maps = client.getSelectedMaps();
			}

			// System.out.println(client+"/"+client.getMapNumber());

			for (MapInfo mi : maps) {
				String txt = mi.getRealName();
				if (cnt == client.getMapNumber() && client.getState().getState() >= NetState.STATE_WAITING) {
					txt = "(*) " + txt;
				}
				if (txt.length() > 21) {
					txt = txt.substring(0, 18) + "...";
				}
				mapRotation.setCell(cnt, 0, txt);
				cnt++;
			}
		}
	}

	private void fillPlayerAndTeamList() {
		if (this.isVisible() && playerList.isVisible()) {
			playerList.clear();
			teamAssignment.clear();
			int cnt = 0;
			if (client.runsServer()) {
				// Get the list from the server...

				List<PlayerInfo> players = client.getServerImpl().getPlayers();
				resize(players);

				for (PlayerInfo pi : players) {
					String name = pi.getName();
					if (pi.isReady() || pi.getClientID() == client.getClientID()) {
						name = "(*) " + name;
					}
					playerList.setCell(cnt, 0, name);
					teamAssignment.setCell(cnt, 0, translateTeam(TeamAssigner.getTeam(pi.getClientID(), pi.getObjectID())));
					cnt++;
				}
				if (cnt != lastPlayerCount && client.getState().getState() <= NetState.STATE_PREPARING) {
					// Something has changed and the game hasn't started yet?
					// Send maps!
					sendMapsToAll();
					lastPlayerCount = cnt;
				}
			} else {
				// Get the list from the client. Less reliable, but should be
				// ok...
				List<Integer> teams = client.getTeams();
				if (teams.size() > 0) {
					resize(teams);
					for (String pi : client.getPlayers()) {
						playerList.setCell(cnt, 0, pi);
						teamAssignment.setCell(cnt, 0, translateTeam(teams.get(cnt)));
						cnt++;
					}
				}
			}
		}
	}

	private void resize(List<?> list) {
		int size = list.size();

		if (size > 16) {
			if (playerList.getRows() != 20) {
				playerListStart = 36;
				replaceTable(20, 7);
			}
			return;
		}

		if (size > 12) {
			if (playerList.getRows() != 16) {
				playerListStart = 38;
				replaceTable(16, 8);
			}
			return;
		}

		if (size > 8) {
			if (playerList.getRows() != 12) {
				playerListStart = 40;
				replaceTable(12, 11);
			}
			return;
		}

		if (size <= 8) {
			if (playerList.getRows() != 8) {
				playerListStart = 42;
				replaceTable(8, 15);
			}
			return;
		}
	}

	private void replaceTable(int newRows, int fontSize) {
		window.remove(playerList);
		window.remove(teamAssignment);

		Table tmp = new Table("Players", newRows, 1, playerListStart + 2, 232, 270, 352);
		tmp.setVisible(playerList.isVisible());
		tmp.setListener(this);
		tmp.setFontSize(fontSize);
		window.add(tmp);
		playerList = tmp;

		tmp = new Table("Team", newRows, 1, 32, 232, 39, 352);
		tmp.setVisible(teamAssignment.isVisible());
		tmp.setListener(this);
		tmp.setFontSize(fontSize);
		window.add(tmp);
		teamAssignment = tmp;
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private String translateTeam(Integer team) {
		if (team == null || team == 0) {
			return "-";
		}
		return team.toString();
	}

	private void setServerGUIElements(boolean enabled) {
		playerList.setVisible(enabled);
		teamAssignment.setVisible(enabled);
		mapRotation.setVisible(enabled);
		addBot.setVisible(enabled);
		addToList.setVisible(enabled);
		lastlevel.setVisible(enabled);
		nextLevel.setVisible(enabled);
		addAll.setVisible(enabled);
		teamListTxt.setVisible(enabled);
		mapRotationTxt.setVisible(enabled);
		previewImg.setVisible(enabled);
		setTeam.setVisible(enabled);
	}

	private void setSharedGUIElements(boolean enabled) {
		tableLabels.setVisible(enabled);
		serverTable.setVisible(enabled);
		portLabel.setVisible(enabled);
		portField.setVisible(enabled);
		connectLabel.setVisible(enabled);
		field.setVisible(enabled);
		name.setVisible(enabled);
		nameLabel.setVisible(enabled);
		startServer.setVisible(enabled);
		ready.setVisible(enabled);
		setTeam.setVisible(enabled);
	}

}
