package robombs.clientserver;

import java.net.*;
import java.util.*;
import java.io.*;

/**
 * As the name indicates, this is a simple server class. Once started, it opens a TCP-socket for clients to connect
 * as well as an (optional) UDP datagram socket (if possible) that broadcasts this server's ip and port for allowing
 * to implement a simple server browser.<br>
 * The communication between this server and its clients is done via DataContainers, which are simple Wrappers for
 * primitive values (and Strings) into byte arrays.
 */
public class SimpleServer {

    /**
     * The default tcp port
     */
    public final static int DEFAULT_PORT = 7000;

    /**
     * The default UDP port
     */
    public final static int UDP_DEFAULT_PORT = 7001;

    private List<DataTransferListener> listener = new ArrayList<DataTransferListener>();
    private List<ClientLogoutListener> logoutListener = new ArrayList<ClientLogoutListener>();
    private List<ClientLoginListener> loginListener = new ArrayList<ClientLoginListener>();
    private Map<ClientInfo, ClientProcessor> clientThreads = new HashMap<ClientInfo, ClientProcessor>();
    private PerformanceCounter pc = new PerformanceCounter("Server");
    private boolean terminate = false;
    private String name = "Default";
    private boolean registerRunning = false;
    private boolean broadcastRunning = false;
    private Thread serverBroadcast=null;
    private int tcpPort=0;

    /**
     * Creates a new Server. When creating a two server, up to two threads are being spawned. One for
     * listening on the tcp socket for clients to connect and one for broadcasting the server's data
     * via UDP.
     * @param tcpPort The tcp port to which a client can connect with this server. This port has to be open and unused or otherwise, the server won't start.
     * @param udpPort the udp port for broadcasting the server's ip and port (and player count). If this port is not available, the broadcasting thread will
     * terminate, but the server will run anyway.
     * @param doBroadcast If true, the broadcasting thread will be spawned. Otherwise, it won't.
     * @param serverName The name of the server. Should be unique but doesn't have to.
     */
    public SimpleServer(int tcpPort, int udpPort, boolean doBroadcast, String serverName) {
        if (serverName != null) {
            name = serverName;
        }
        this.tcpPort=tcpPort;
        new Thread(new ClientRegisterService(tcpPort)).start();
        if (doBroadcast) {
        	serverBroadcast=new Thread(new ServerBroadcast(tcpPort, udpPort));
        	serverBroadcast.start();
        }
    }

    /**
     * Adds a new DataTransferListener to the server. A DataTransferListener will be notified in case of data
     * being transfered from the client to the server.
     * @param sl the listener
     */
    public void addListener(DataTransferListener sl) {
        listener.add(sl);
    }

    /**
     * Adds a new ClientLogoutListener to the server. This listener will be notified if a client logs out
     * from this server.
     * @param ctl the listener
     */
    public void addLogoutListener(ClientLogoutListener ctl) {
        logoutListener.add(ctl);
    }

    /**
     * Adds a new ClientLoginListener to the server. This listener will be notified if a client logs in
     * on this server.
     * @param ctl the listener
     */
    public void addLoginListener(ClientLoginListener ctl) {
        loginListener.add(ctl);
    }

    public int getPort() {
    	return tcpPort;
    }
    
    /**
     * Shuts down the server. This shut down is semi-hard, i.e. all server threads and such will be terminated correctly, but
     * there's no waiting for the clients to be informed about this. The clients have to take care of a "lost" server themselves.
     */
    public void shutDown() {
        terminate = true;
        if (serverBroadcast!=null) {
        	serverBroadcast.interrupt();
        }
        try {
            while (registerRunning || broadcastRunning) {
                Thread.sleep(20);
            }
        } catch (Exception e) {
            // Who cares...?
            e.printStackTrace();
        }
    }
    
    public int getClientCount() {
    	return clientThreads.size();
    }

    /**
     * The broadcast thread. The broadcast goes to all clients in the local subnet via UDP.
     * If the port is not available, this thread will terminate with a RuntimeException but the
     * server will continue to run without it.
     * Currently, the server will send its data into the network every three seconds.
     */
    private class ServerBroadcast implements Runnable {

        private int port = 0;
        private int tcpPort = 0;

        public ServerBroadcast(int tcpPort, int port) {
            this.port = port;
            this.tcpPort = tcpPort;
        }

        public void run() {
            try {
                DatagramSocket bsock = new DatagramSocket(port);
                InetAddress bc = InetAddress.getByName("255.255.255.255");
                broadcastRunning = true;

                while (!terminate) {
                    try {
                        NetLogger.log("Server: Broadcasting server data!");
                        send(bc, bsock, false);
                        Thread.sleep(3000);
                    } catch (Exception e) {
                        // Don't stop...just take notice!
                        NetLogger.log("Server: Broadcast interrupted!");
                    }
                }
                
                // Send exit-information to the browser multiple times to increase the chance
                // that he gets this information (it's UDP, so...)
                for (int i=0; i<2; i++) {
                	send(bc, bsock, true);
                	if (i==0) {
                		try {
                			Thread.sleep(100);
                		} catch (Exception e) {
                		}
                	} 
                }
                
                bsock.close();
                broadcastRunning = false;
                NetLogger.log("Server: Broadcast thread terminated!");

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        
        private void send(InetAddress bc, DatagramSocket bsock, boolean exit) throws Exception {
       	 DataContainer dc = new DataContainer();
            dc.add(name);
            dc.add(tcpPort);
            if (!exit) {
            	dc.add(clientThreads.size());
            } else {
            	// This flags the server browser, that this server goes down.
            	dc.add(-9999);
            }
            byte[] bytes = DataContainerFactory.toByteArray(new DataContainer[] {dc}, false);
            DatagramPacket dps = new DatagramPacket(bytes, bytes.length, bc, port + 1);
            bsock.send(dps);
       }
        
    }

    

    /**
     * This is the server's listening thread. It waits for client connections on the specified tcp port and spawns a new
     * thread for each client that handles this client's requests and responses. If this thread dies (or doesn't start up
     * due to the port not being available), the server in not running.
     */
    private class ClientRegisterService implements Runnable {

        private int port = 0;

        public ClientRegisterService(int port) {
            this.port = port;
        }

        public void run() {
            try {
                ServerSocket socket = new ServerSocket(port);
                socket.setSoTimeout(1000);
                registerRunning = true;
                NetLogger.log("Server: Listening for client requests at port: " + port);
                while (!terminate) {
                    try {
                        Socket sock = socket.accept();
                        NetLogger.log("Server: Request from client " + sock.getInetAddress() + "!");
                        InputStream is = sock.getInputStream();
                        OutputStream os = sock.getOutputStream();

                        byte[] bytes = StreamConverter.convert(is);

                        DataContainer[] cs = DataContainerFactory.extractContainers(bytes, false);

                        DataContainer c = cs[0];
                        DataContainer add = null;
                        if (cs.length > 1) {
                            add = cs[1];
                        }
                        DataContainer res = new DataContainer();
                        boolean ok = false;
                        if (c.hasData()) {
                            NetLogger.log("Server: Checking request type!");

                            int command = c.getMessageType();
                            // Login
                            if (command == MessageTypes.LOGIN_REQUEST) {
                                ClientInfo ci = new ClientInfo(sock.getInetAddress(), sock.getPort());
                                if (!clientThreads.containsKey(ci)) {
                                    byte zip = c.getNextByte();
                                    NetLogger.log("Server: Request type is 'login', clientID will be " + ci.getID() + ", zip=" + (zip == 1) + "!");
                                    ci.setZipMode(zip == 1);
                                    res.setMessageType(MessageTypes.LOGIN_SUCCESS);
                                    res.add(ci.getID());

                                    ClientProcessor cp = new ClientProcessor(sock, ci);
                                    clientThreads.put(ci, cp);

                                    ok = true;

                                    for (Iterator<ClientLoginListener> itty = loginListener.iterator(); itty.hasNext(); ) {
                                        ClientLoginListener cll = itty.next();
                                        DataContainer resp = cll.loggedIn(ci, add);
                                        if (resp != null) {
                                            broadcast(resp);
                                        } else {
                                            ok = false;
                                            break;
                                        }
                                    }

                                    if (ok) {
                                        new Thread(cp).start();
                                    }
                                } else {
                                    // @todo: May this happen?
                                }
                            }
                        }
                        if (!ok) {
                            res.setMessageType(MessageTypes.LOGIN_FAILURE);
                            NetLogger.log("Server: Request type is 'unknown' or no request type found (" + c.hasData() + ")!");
                        }
                        // Send response
                        os.write(DataContainerFactory.toByteArray(new DataContainer[] {res}, false));
                        os.flush();
                    } catch (SocketTimeoutException se) {
                        // Nobody cares...
                    } catch (Exception e) {
                        // This thread must not die. But it may print out its opinions on the current situation...
                        e.printStackTrace();
                    }
                }
                socket.close();
                registerRunning = false;
                NetLogger.log("Server: Register thread terminated!");
            }

            catch (Exception e) {
            	registerRunning=false;
                throw new RuntimeException(e);
            }
        }
    }


    /**
     * Adds a DataContainer to the broadcast-queue. Each container added to this queue is ensured to be send
     * once to all clients known to the server at the time that this method is being called (unless the client
     * hasn't died or logout, of course).<br>
     * This has nothing to do with the data broadcast via UDP that the ServerBroadcast is doing. This is a tcp
     * "broadcast" to all known clients.
     * @param dc the DataContainer that should be enqueued
     */
    public void broadcast(DataContainer dc) {
        if (dc != null) {
            for (ClientProcessor cpt: clientThreads.values()) {
                cpt.hasToSend(dc);
            }
        }
    }
    
    public void broadcastToOthers(DataContainer dc, int clientID) {
        if (dc != null) {
            for (ClientProcessor cpt: clientThreads.values()) {
            	if (cpt.ci.getID()!=clientID) {
            		cpt.hasToSend(dc);
            	}
            }
        }
    }
    
    /**
     * Adds a DataContainer to the broadcast-queue of a single client. Each container added to this queue is ensured to be send
     * once to the client.
     * @param dc the DataContainer that should be enqueued
     */
    public void sendToSingleClient(DataContainer dc, int clientID) {
        if (dc != null) {
            for (ClientProcessor cpt: clientThreads.values()) {
                if (cpt.ci.getID()==clientID) {
                	cpt.hasToSend(dc);
                }
            }
        }
    }
    
    public void setTimeOut(int timeOut) {
        for (ClientProcessor cpt: clientThreads.values()) {
            cpt.setTimeOut(timeOut);
        }
    }


    /**
     * This is the worker thread. Each client connecting will cause an instance of this to be spawned. Each client's
     * thread terminates if the client logs or times out. Time out time is three seconds.
     */
    private class ClientProcessor implements Runnable {

        private InputStream is = null;
        private OutputStream os = null;
        private ClientInfo ci = null;
        private Socket sock = null;
        private volatile int timeOut=3000;
        private boolean exit = false;
        private List<DataContainer> hasToSend = new ArrayList<DataContainer>();

        /**
         * Creates a instance with the given socket and the ClientInfo that identifies this client.
         * @param sock the socket to which is client is connected
         * @param ci the ClientInfo
         * @throws Exception if anything goes wrong...
         */
        public ClientProcessor(Socket sock, ClientInfo ci) throws Exception {
            sock.setSoTimeout(NetGlobals.serverTimeOut);
            sock.setTcpNoDelay(NetGlobals.lowLatency);
            sock.setPerformancePreferences(0, 2, 1);
            this.is = sock.getInputStream();
            this.os = sock.getOutputStream();
            this.ci = ci;
            this.sock = sock;
        }

        /**
         * Stops a client's thread and removes the client from the server's clients list.
         */
        public void kill() {
            exit = true;
        }

        /**
         * Used to enqueue a DataContainer into this client's broadcast queue.
         * @param dc the container
         */
        public void hasToSend(DataContainer dc) {
            if (dc != null) {
                synchronized (hasToSend) {
                    hasToSend.add(dc);
                }
            }
        }

        public void setTimeOut(int t) {
        	try {
        		sock.setSoTimeout(t);
        		timeOut=t;
        	} catch(Exception e) {
        		NetLogger.log("Unable to set timeout for socket!");
        	}
        }
        
        public void run() {
        	Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
            try {
            	DataContainer[] crs = new DataContainer[1];
            	while (!exit && !terminate) {
                	long start=System.nanoTime();
                    boolean recd = false;
                    DataContainer c = null;
                    DataContainer[] cs = null;
                    int lastTimeOut=timeOut;
                    try {
                    	byte[] bytes = StreamConverter.convert(is);
                        cs = DataContainerFactory.extractContainers(bytes, ci.getZipMode());
                        recd = true;
                    } catch (Exception e) {
                    	if (timeOut==lastTimeOut) {
                    		// If the timeout has changed in between, this is no problem...
	                        e.printStackTrace();
	                        NetLogger.log("Server: Client lost (timeout?)!");
	                        logout(ci);
	                        exit = true;
                    	}
                    }
                    
                    pc.printStats();
                    if (recd) {

                        List<DataContainer> res = new ArrayList<DataContainer>();
                        for (int i = 0; i < cs.length; i++) {
                            c = cs[i];
                            pc.in(c.getLength());
                            c.setClientInfo(ci);
                            int msgType = c.getMessageType();

                            if (msgType <= MessageTypes.INTERNAL_BORDER) {
                                // "Normal" transmission, i.e. no internal server message
                                for (DataTransferListener sl:listener) {
                                    DataContainer[] crss = sl.dataReceived(c, msgType);
                                    if (crss != null) {
                                        res.addAll(Arrays.asList(crss));
                                    }
                                }
                            } else {
                                // an internal server message
                                processMessage(msgType, res);
                            }
                        }

                        // Notify all listeners that data has been received...
                        for (DataTransferListener sl:listener) {
                            sl.dataReceivedEnd();
                        }

                        // Send response to the client...
                        int pos = 0;
                        synchronized (hasToSend) {
                        	
                        	int len=res.size() + hasToSend.size();
                        	if (len!=crs.length) {
                        		// Create a new one only if needed!
                        		crs = new DataContainer[len];
                        	}

                            // Append all responses from the transfer listeners...
                            for (DataContainer cr:res) {
                                crs[pos] = cr;
                                pos++;
                            }
                            // Append all data from the broadcast queue...
                            for (DataContainer dhts:hasToSend) {
                                if (dhts == null) {
                                    throw new RuntimeException("Null value in broascast queue at position " + (pos - 1));
                                }
                                crs[pos] = dhts;
                                pos++;
                            }
                            hasToSend.clear();
                        }

                        byte[] bytes = DataContainerFactory.toByteArray(crs, ci.getZipMode());
                        os.write(bytes);
                        pc.out(bytes.length);

                        os.flush();
                        ci.touch();
                    }
                    // Each thread runs once every xx milliseconds.
                    long end=System.nanoTime();
                    end=(end-start)/1000000L;
                    
                    if (end>250) {
                    	NetLogger.log("Server: Server lags ("+end+"ms)!");
                    }
                    
                    long st=Math.min(NetGlobals.serverWaitTime, Math.max(0,NetGlobals.serverWaitTime-end));
                    if (st>=0) {
                    	Thread.sleep(st);
                    } else {
                    	Thread.yield();
                    }
                }

                // This client has logged out, died, failed...whatever...we are done with it.

                logout(ci);

                is.close();
                os.close();
                sock.close();
                NetLogger.log("Server: Thread for client " + ci + " terminated!");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            finally {
              clientThreads.remove(ci);
            }
        }

        /**
         * Processes an internal server message. Currently, this can be either log out...or log out...:-)
         * @param msgType int
         * @param res List
         */
        private void processMessage(int msgType, List<DataContainer> res) {
            DataContainer cr = new DataContainer();
            cr.setZip(ci.getZipMode());

            if (msgType == MessageTypes.LOGOUT_REQUEST) {
                NetLogger.log("Server: Request type is 'logout' from client " + ci + "!");
                cr.setMessageType(MessageTypes.LOGOUT_SUCCESS);
                kill();
                logout(ci);
            }
            res.add(cr);
        }
    }

    
    public void logout(int cid) {
    	for (ClientInfo ci:clientThreads.keySet()) {
    		if (ci.getID()==cid) {
    			logout(ci);
    			break;
    		}
    	}
    }
    
    public boolean isRunning() {
    	return registerRunning;
    }
    
    /**
     * Logout a client by notifying all listeners and adding their responses to the broadcast
     * queue.
     * @param ci the client to log out
     */
    private void logout(ClientInfo ci) {
        if (clientThreads.containsKey(ci) && !ci.isLoggedOut()) {
            for (Iterator<ClientLogoutListener> itty2 = logoutListener.iterator(); itty2.hasNext(); ) {
                ClientLogoutListener ctl = itty2.next();
                DataContainer dc = ctl.loggedOut(ci);
                broadcast(dc);
                NetLogger.log("Server: Client " + ci.getAddress() + " logged out!");
            }
            ci.logout();
        }
    }
}
