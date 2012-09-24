package robombs.game;

import java.util.*;

import robombs.clientserver.*;
import robombs.game.model.*;
import robombs.game.util.*;

/**
 * This is this test game's server component. It's usually started from within a running client but can run as
 * a stand-alone server as well.
 */
public class BlueThunderServer implements DataTransferListener, Runnable, ClientLoginListener, ClientLogoutListener {

    private final static Object SYNC=new Object();

    private boolean exit = false;

    private SimpleServer serverImpl=null;
    private ExtendedDataContainer serverState=null;
    private ServerObjectManager serverObjMan=null;
    private ServerEventManager eventMan=null;

    private int port=0;
    private Map<Integer, List<PlayerInfo>> client2Info=new HashMap<Integer, List<PlayerInfo>>();
    private Map<Integer, NetState> client2State=new HashMap<Integer, NetState>();
    private int lastLevelChksum=-9999;
    private boolean playing=false;
    private MapList mapList=null;
    private boolean running=false;
    
    @SuppressWarnings("unused")
    private List<MapInfo> selectedMaps=null;
    
    /**
     * Starts a new server using the default tcp-port.
     */
    public BlueThunderServer() {
        this(SimpleServer.DEFAULT_PORT);
    }

    /**
     * Starts a new server using a specified tcp-port.
     * @param port int the port
     */
    public BlueThunderServer(int port) {
        this.port=port;
        mapList=new MapList();
        NetLogger.log("Server: Magic number is "+mapList.getCheckSum());
    }

    /**
     * Run the server as stand-alone.
     * @param args String[]
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        BlueThunderServer str = new BlueThunderServer();
        str.run();
    }
    
    public void setTimeOut(int t) {
    	serverImpl.setTimeOut(t);
    }
    
    public void reset() {
    	serverObjMan.reset();
    }
    
    public void setSelectedMaps(List<MapInfo> lst) {
    	selectedMaps=new ArrayList<MapInfo>(lst);
    }
    
    public int getClientCount() {
      return client2Info.size();
    	//return serverImpl.getClientCount();
    }
    
    public void setGameState(boolean gameRunning) {
    	playing=gameRunning;
    }
    
    public List<PlayerInfo> getPlayers() {
    	List<PlayerInfo> res=new ArrayList<PlayerInfo>();
    	for (List<PlayerInfo> lst:client2Info.values()) {
    		res.addAll(lst);
    	}
    	Collections.sort(res);
    	return res;
    }
    
    public boolean getGameState() {
    	return playing;
    }
    
    public int getStateCount(int state) {
    	int cnt=0;
    	for (NetState ns:client2State.values()) {
    		if (ns.getState()==state) {
    			cnt++;
    		}
    	}
    	return cnt;
    }
    
    public List<PlayerInfo> getWithoutState(int state) {
    	for (Integer i:client2State.keySet()) {
    		NetState ns=client2State.get(i);
    		if (ns.getState()!=state) {
    			return getPlayerInfo(i);
    		}
    	}
    	
    	return null;
    }

    public void logout(int cid) {
    	serverImpl.logout(cid);
    }
    
    public DataContainer loggedOut(ClientInfo ci) {
        InfoDataContainer dc=new InfoDataContainer();
        Integer key=Integer.valueOf(ci.getID());
        List<PlayerInfo> pis= client2Info.get(key);
        if (pis!=null) {
	        for (PlayerInfo pi:pis) {
		        if (pi!=null) {
		            InfoLine il=new InfoLine(InfoLine.PLAYER_REMOVED, 0, String.valueOf(ci.getID()), pi.getName());
		            dc.add(il);
		            il=new InfoLine(InfoLine.SYSTEM_OUT, 0, "msg", "Player '"+pi.getName()+"' has left the game!");
		            dc.add(il);
		            TeamAssigner.removeTeamAssigment(ci.getID(), pi.getObjectID());
		        } else {
		            // This may happen, if the game is already running. Do we have to do something here?
		        }
	        }
        }

        serverObjMan.remove(ci);
        client2Info.remove(key);
        client2State.remove(key);
        
        addScores(dc);
        
        return dc;
    }
    
    public void setStateForAll(int state) {
    	for (NetState ns:client2State.values()) {
    		ns.setState(state);
    	}
    }
    
    public void setState(int id, int state) {
    	NetState ns=client2State.get(Integer.valueOf(id));
    	if (ns!=null) {
    		ns.setState(state);
    	} else {
    		throw new RuntimeException("Can't set the state for an unknown client: "+id+"!");
    	}
    }
    
    public int getPort() {
    	return serverImpl.getPort();
    }

    public DataContainer loggedIn(ClientInfo ci, DataContainer dc) {
    	if (!getGameState()) {
	        String playerName="player";
	        int chkSum=-1;
	        int botFlag=0;
	        int objID=0;
	        if (dc!=null) {
	            String tmp=dc.getNextString();
	            if (tmp!=null && tmp.length()>0) {
	                playerName = tmp;
	            }
	            chkSum=dc.getNextInt();
	            botFlag=dc.getNextInt();
	            objID=dc.getNextInt();
	        }
	        if (chkSum!=mapList.getCheckSum()) {
	        	// Levels differ!
	        	EventDataContainer edc=new EventDataContainer();
	    		Event e=new Event(Event.LOGIN_REJECTED, -98, -98, ci.getID());
	    		e.setSourceClientID(ci.getID());
		        edc.add(e);
		        Integer key=Integer.valueOf(ci.getID());
		        NetState ns=new NetState();
		        ns.setState(NetState.STATE_NOT_CONNECTED);
		        client2State.put(key, ns);
		        NetLogger.log("Client "+ci.getID()+" rejected. Levels are different ("+mapList.getCheckSum()+"!="+chkSum+")!");
		        return edc;
	        } else {
	        	Integer key=Integer.valueOf(ci.getID());
	        	
		        NetState ns=new NetState();
		        ns.setState(NetState.STATE_CONNECTED);
		        client2State.put(key, ns);
		        if (botFlag==0) {
		        	addPlayer(playerName, ci.getID(), objID, false);
		        }
		        String txt="'"+playerName+"' has logged in!";
		        NetLogger.log(txt);
		        InfoDataContainer ic=new InfoDataContainer();
		        InfoLine il=new InfoLine(InfoLine.SYSTEM_OUT, 0, "msg", txt);
		        ic.add(il);
		        return ic;
	        }
    	} else {
    		EventDataContainer edc=new EventDataContainer();
    		Event e=new Event(Event.LOGIN_REJECTED, -99, -99, ci.getID());
    		e.setSourceClientID(ci.getID());
	        edc.add(e);
	        Integer key=Integer.valueOf(ci.getID());
	        NetState ns=new NetState();
	        ns.setState(NetState.STATE_NOT_CONNECTED);
	        client2State.put(key, ns);
	        NetLogger.log("Client "+ci.getID()+" rejected. The game is already running!");
	        return edc;
    	}
    }
    
    public void sendScores() {
    	InfoDataContainer ic=new InfoDataContainer();
    	addScores(ic);
        broadcast(ic);
    }
    
    public void removePlayer(PlayerInfo pi) {
    	List<PlayerInfo> lst=client2Info.get(pi.getClientID());
    	for (Iterator<PlayerInfo> itty=lst.iterator(); itty.hasNext();) {
    		PlayerInfo pit=itty.next();
    		if (pi.getObjectID()==pit.getObjectID()) {
    			itty.remove();
    			break;
    		}
    	}
    	
    	String txt="'"+pi.getName()+"' has left the game!";
        NetLogger.log(txt);
        InfoDataContainer ic=new InfoDataContainer();
        InfoLine il=new InfoLine(InfoLine.SYSTEM_OUT, 0, "msg", txt);
        ic.add(il);
        addScores(ic);
        broadcast(ic);
    }
    
    public synchronized void addPlayer(String name, int cid, int oid, boolean isBot) {
    	PlayerInfo pi=new PlayerInfo(name, cid, oid);
    	addToPlayerInfoList(Integer.valueOf(cid), pi);
    	pi.setBot(isBot);
    	if (isBot) {
    		pi.ready(true);
    	}
    	
    	String txt="'"+name+"' has joined the game!";
        NetLogger.log(txt);
        InfoDataContainer ic=new InfoDataContainer();
        InfoLine il=new InfoLine(InfoLine.PLAYER_ADDED, 0, "name", name);
        ic.add(il);
        il=new InfoLine(InfoLine.SYSTEM_OUT, 0, "msg", txt);
        ic.add(il);
        addScores(ic);
        broadcast(ic);
    }
    
    public boolean checkLevel(DataContainer dc) {
    	int chk=dc.getNextInt();
        // very simple way to prove that the level is the same on all clients.
        // the first one with a different level will be rejected!
        if (lastLevelChksum!=-9999 && chk!=lastLevelChksum) {
            return false;
        }
        lastLevelChksum=chk;
        return true;
    }

    public void run() {
        try {
            initServer();
            runServer();
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Stops the server.
     */
    public void stop()  {
        exit=true;
        running=false;
        serverImpl.shutDown();
    }

    public void dataReceivedEnd() {}

    /**
     * Gets the players' info to a client's ID.
     * @param clientID int the ID
     * @return PlayerInfo the player info
     */
    public synchronized List<PlayerInfo> getPlayerInfo(int clientID) {
        return client2Info.get(Integer.valueOf(clientID));
    }

    public PlayerInfo getPlayerInfo(int clientID, int objectID) {
    	List<PlayerInfo> pis=getPlayerInfo(clientID);
    	if (pis!=null) {
	    	for (PlayerInfo pi:pis) {
	    		if (pi.getObjectID()==objectID) {
	    			return pi;
	    		}
	    	}
    	}
    	NetLogger.log("Server: Unable to find PlayerInfo for "+clientID+"/"+objectID+"!?");
    	return null;
    }
    
    /**
     * Broadcasts data to all clients.
     * @param dc DataContainer the container with the data to broadcast
     */
    public void broadcast(DataContainer dc) {
        serverImpl.broadcast(dc);
    }
    
    public void broadcastToOthers(DataContainer dc, int clientID) {
        serverImpl.broadcastToOthers(dc, clientID);
    }
    
    public void sendToSingleClient(DataContainer dc, int clientID) {
        serverImpl.sendToSingleClient(dc, clientID);
    }

    /**
     * Removes an object from the server
     * @param loID int
     * @param clientID int
     * @param ci ClientInfo
     */
    public void remove(int loID, int clientID, ClientInfo ci) {
        synchronized(SYNC) {
            LocalObject lo=serverObjMan.getLocalObjectToIDs(loID, clientID);
            serverObjMan.removeObject(lo, ci);
        }
    }

    public DataContainer[] dataReceived(DataContainer c, int type) {
       try {
    	   if (c != null) {
               synchronized(SYNC) {
                   ExtendedDataContainer ec = new ExtendedDataContainer(c);
                   if (type==MessageTypes.OBJ_TRANSFER) {
                       while (ec.hasData()) {
                           serverObjMan.setOrUpdate(ec);
                       }
                       if (!Globals.activeServer) {
                    	   return new DataContainer[] {(DataContainer) serverState.clone()};
                       } else {
                    	   return new DataContainer[] {createServerState(ec.getClientInfo())};
                       }
                   }
                   if (type==MessageTypes.EVENT) {
                       EventDataContainer edc=new EventDataContainer(ec);
                       List<DataContainer> resp=new ArrayList<DataContainer>();
                       ClientInfo ci=edc.getClientInfo();
                       while (edc.hasData()) {
                           DataContainer[] res=eventMan.manageEvent(edc.getEvent(), serverObjMan, this, ci);
                           if (res!=null) {
                               resp.addAll(Arrays.asList(res));
                           }
                       }
                       if (resp.size()==0) {
                           return null;
                       }
                       DataContainer[] res=new DataContainer[resp.size()];
                       for (int i=0; i<res.length; i++) {
                           res[i]=(DataContainer) resp.get(i);
                       }
                       return res;
                   }
               }
           }
       } catch(Exception e) {
           throw new RuntimeException(e);
       }
       return null;
    }

    /**
     * Adds the scores to a highscore table (a GUI-element).
     * @param idc InfoDataContainer the info container containing the scores
     */
    public void addScores(InfoDataContainer idc) {
        HighscoreTable hi = new HighscoreTable();
        for (List<PlayerInfo> lst:client2Info.values()) {
	        for (Iterator<PlayerInfo> itty = lst.iterator(); itty.hasNext(); ) {
	            PlayerInfo pi = itty.next();
	            hi.addLine(pi);
	        }
        }
        if (idc!=null) {
        	hi.addToContainer(idc);
        }
    }
    
    public boolean isRunning() {
    	return running && serverImpl.isRunning();
    }

    private synchronized void addToPlayerInfoList(Integer cid, PlayerInfo pi) {
    	List<PlayerInfo> lst=client2Info.get(cid);
    	if (lst==null) {
    		lst=new ArrayList<PlayerInfo>();
    		client2Info.put(cid, lst);
    	}
    	lst.add(pi);
    }
    
    /**
     * Initialize the server
     */
    private void initServer() {
    	TeamAssigner.clear();
        serverState=new ExtendedDataContainer();
        String name=System.getProperty("user.name");
        if (name==null) {
            name="My server";
        } else {
            if (name.endsWith("s")) {
                name+="'";
            } else {
                name+="'s";
            }
            name+=" server";
        }
        serverImpl=new SimpleServer(port,SimpleServer.UDP_DEFAULT_PORT,true, name);
        serverImpl.addListener(this);
        serverObjMan=new ServerObjectManager();
        serverImpl.addLogoutListener(this);
        serverImpl.addLoginListener(this);
        eventMan=new ServerEventManager();
        running=true;
    }

    /**
     * Creates a new "server state". This means that all the data living on the server gets collected
     * and stored in a structure ready to be transfered to the clients. This method is intended for
     * fixed time frame usage.
     */
    private void updateServerState() {
        synchronized(SYNC) {
            serverState=new ExtendedDataContainer();
            // Transfer all client data
            serverObjMan.fill(serverState, null);
        }
    }
    
    /**
     * Similar to above, but it excludes the client's own objects. This is intended for creating the
     * data on demand.
     * @param ci
     * @return
     */
    private ExtendedDataContainer createServerState(ClientInfo ci) {
    	//synchronized(SYNC) {
    		ExtendedDataContainer serverState=new ExtendedDataContainer();
            // Transfer all client data
            serverObjMan.fill(serverState, ci);
            return serverState;
        //}
    }

    /**
     * The server's main loop.
     * @throws Exception
     */
    private void runServer() throws Exception {
    	if (!Globals.activeServer) {
    		// For an active Server (i.e. one that creates the data for each client on request not
    		// in fixed intervals, this method does nothing but returns
	    	Ticker ticker = new Ticker(18);
	
	        while (!exit) {
	            int ticks = ticker.getTicks();
	            if (ticks > 0) {
	                updateServerState();
	            } else {
	                Thread.sleep(5);
	            }
        	}
        	stop();
    	}
    }
}
