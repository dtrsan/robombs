package robombs.clientserver;

import java.net.*;
import java.io.*;
import java.util.*;

/**
 * This the client's counterpart to the SimpleServer. If connects to a SimpleServer and exchanges DataContainers with it.
 */
public class SimpleClient {

    private boolean exit = false;
    private List<DataTransferListener> listener = new ArrayList<DataTransferListener>();
    private List<ClientPreProcessor> pres = new ArrayList<ClientPreProcessor>();
    private DataContainer[] nextData = null;
    private int myID = -1;

    private String server = null;
    private int port = 0;
    private boolean zip = false;
    private Sender sender = null;
    private Thread senderThread=null;
    private boolean connected = false;
    private DataContainer initialDG = null;
    private volatile boolean waitingForData=false;

    private PerformanceCounter pc = new PerformanceCounter("Client");

    private final static Object SYNC = new Object();

    /**
     * Creates a new SimpleClient. The client won't connect to the server unless the connect()-method has been called.
     * @param server the address of the server. This can be IPv4, IPv6 or a name.
     * @param port the port on which the server is listening for clients to connect
     * @param zip if true, all transfers (except for the login itself) will be zipped. This consumes more cpu power but reduces bandwidth usage.
     * @param data an initial DataContainer. This container will be passed to the registered ClientLoginListeners (if any) by the server. The SimpleServer itself won't process it.
     */
    public SimpleClient(String server, int port, boolean zip, DataContainer data) {
        this.server = server;
        this.port = port;
        this.zip = zip;
        this.initialDG = data;
    }

    /**
     * Creates a new SimpleClient. The client won't connect to the server unless the connect()-method has been called.
     * @param se the ServerEntry (usually taken from the ServerBrowser) that holds the server's data.
     * @param zip if true, all transfers (except for the login itself) will be zipped. This consumes more cpu power but reduces bandwidth usage.
     * @param data an initial DataContainer. This container will be passed to the registered ClientLoginListeners (if any) by the server. The SimpleServer itself won't process it.
     */
    public SimpleClient(ServerEntry se, boolean zip, DataContainer data) {
        this(se.getAddress().getHostName(), se.getPort(), zip, data);
    }

    /**
     * Tries to connect to the configured server. If that fails, an Exception will be thrown.
     * @throws Exception failed?
     */
    public void connect() throws Exception {
        sender = new Sender();
        senderThread=new Thread(sender);
        senderThread.start();
    }

    /**
     * Disconnects from a server. If there is no connection established, nothing will be done.
     */
    public void disconnect() {
        if (sender != null) {
            sender.disconnect();
        }
        sender = null;
    }
    
    public String getServer() {
    	String ip=sender.conn.toString();
    	int pos=ip.indexOf("/");
    	if (pos!=-1) {
    		ip=ip.substring(pos+1);
    	}
    	return ip.trim();
    }

    /**
     * Adds a DataTransferListener to the client. This listener will be called when the client receives data from the server.
     * @param sl the listener
     */
    public void addListener(DataTransferListener sl) {
        listener.add(sl);
    }

    /**
     * Adds a ClientPreProcessor to the client. A ClientPreProcessor will be called before sending to the server, before receiving
     * from the server and after receiving from the server to allow its implementing class to do whats needed in these stages. For example,
     * "before sending" should prepare the data to be send to the server and inject it into the SimpleClient.
     * @param cpp ClientPreProcessor
     */
    public void addPreProcessor(ClientPreProcessor cpp) {
        pres.add(cpp);
    }

    /**
     * Sets the data to be transfered to the server in the next transfer. This is usually called from inside an implementation
     * of a ClientPreProcessor's beforeSending()-method.
     * @param c an array of DataContainers
     */
    public void setContainers(DataContainer[] c) {
       synchronized (SYNC) {
           nextData = c;
       }
       if (waitingForData) {
    	   // Already waiting for data? Interrupt that...
    	   senderThread.interrupt();
       }
   }

   /**
    * Returns this client's id. Once connected, this is a positive integer that is unique on the server.
    * @return int the id
    */
   public int getClientID() {
        return myID;
    }

    /**
     * Is this client connected to a server?
     * @return boolean is it?
     */
    public boolean isConnected() {
        return connected;
    }
    
    /**
     * Make the client thread process new data regardless if he's waiting or not...
     * This increases network load but smoothes movement of remote entities.
     */
    public void triggerTransfer() {
    	senderThread.interrupt();
    }

    /**
     * The working thread for a client. In this thread, data is being sent and received to/from the server.
     */
    private class Sender implements Runnable {

        private Socket conn = null;
        private InputStream is = null;
        private OutputStream os = null;
        private boolean disconnect = false;

        /**
         * Connects to the server with the configured address/port.
         * @throws Exception if the connection fails, the client thread fails...
         */
        public Sender() throws Exception {
            NetLogger.log("Trying to connect to " + server + ":" + port);
            conn = new Socket(server, port);
            conn.setSoTimeout(10000);
            conn.setTcpNoDelay(NetGlobals.lowLatency);
            conn.setPerformancePreferences(0, 2, 1);
            os = conn.getOutputStream();
            is = conn.getInputStream();
            connect();
        }

        /**
         * Disconnects the client from the server. This is done sending a logout request to the server and then
         * terminating the thread. If the server gets the request, it can't vote against it, i.e. the client
         * will be disconnected in every case.
         */
        private void disconnect() {
            disconnect = true;
        }

        /**
         * Tries to disconnect from the server. If this isn't possible, this is not an error. Maybe it has already been
         * done before or the server is dead...
         */
        private void disconnectInternal() {
            try {
                NetLogger.log("Trying to disconnect from " + server + ":" + port);
                DataContainer c = new DataContainer();
                c.setMessageType(MessageTypes.LOGOUT_REQUEST);
                os.write(DataContainerFactory.toByteArray(new DataContainer[] {c}, false));
                os.flush();
                c = new DataContainer(StreamConverter.convert(is));
                if (c.getMessageType() == MessageTypes.LOGOUT_SUCCESS) {
                    NetLogger.log("Client: Disconnected!");
                } else {
                    NetLogger.log("Client: Unexpected server response!");
                }
            } catch (Exception e) {
                NetLogger.log("Client: Unable to disconnect (already disconnected?)!");
            }
            exit = true;
        }

        /**
         * Connects to the server
         * @throws Exception
         */
        private void connect() throws Exception {
            boolean con = false;
            myID = -1;

            while (!con) {
                DataContainer c = new DataContainer();
                c.setMessageType(MessageTypes.LOGIN_REQUEST);
                if (zip) {
                    c.add((byte) 1);
                } else {
                    c.add((byte) 0);
                }

                DataContainer[] cont = null;
                if (initialDG != null) {
                    cont = new DataContainer[] {c, initialDG};
                } else {
                    cont = new DataContainer[] {c};
                }

                os.write(DataContainerFactory.toByteArray(cont, false));
                c = new DataContainer(StreamConverter.convert(is));

                boolean ok = false;
                if (c.getMessageType() == MessageTypes.LOGIN_SUCCESS) {
                    ok = true;
                    con = true;
                    myID = c.getNextInt();
                    pc = new PerformanceCounter("Client "+myID);
                    NetLogger.log("Client: Logged in - ID is: " + myID + "!");
                    connected = true;
                }
                if (!ok) {
                    NetLogger.log("Client: Error logging in!");
                }
                if (!con) {
                    // retry!
                    Thread.sleep(200);
                }
            }
        }

        public void run() {
        	Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
            try {
            	while (!exit) {
            		long start=System.nanoTime();
                    pc.printStats();
                    synchronized (SYNC) {
                        // Call the PreProcessors. It's most likely that those will create the Packet to
                        // send or something...
                        for (ClientPreProcessor cpp:pres) {
                            cpp.beforeSending();
                        }
                    }
                    byte[] bytes = null;

                    synchronized (SYNC) {
                        if (nextData != null) {
                            bytes = DataContainerFactory.toByteArray(nextData, zip);
                            nextData=null;
                        } 
                    }

                    DataContainer c = null;
                    DataContainer[] cs = null;

                    if (bytes != null) {
                        if (!disconnect) {
                            os.write(bytes);
                            pc.out(bytes.length);
                            os.flush();
                            boolean ok = true;
                            byte[] res = null;

                            try {
                                synchronized (SYNC) {
                                    for (ClientPreProcessor cpp:pres) {
                                        cpp.beforeReceiving();
                                    }
                                }
                                
                                res = StreamConverter.convert(is);
                                if (res == null || res.length == 0) {
                                    NetLogger.log("Client: Got a zero-sized response from server. Trying to disconnect now!");
                                    disconnectInternal();
                                    ok = false;
                                }
                                pc.in(res.length);
                                
                            } catch (Exception e) {
                            	e.printStackTrace();
                                NetLogger.log("Client: Server error (server running?)!");
                                disconnectInternal();
                                ok = false;
                            }
                            /*
                            if (Globals.emulateRemoteServer) {
                    			try {
                    				Thread.sleep(30);
                    			} catch(Exception e){}
                    		}*/
                            
                            if (ok) {
                                cs = DataContainerFactory.extractContainers(res, zip);
                                
                                for (int i = 0; i < cs.length; i++) {
                                    c = cs[i];
                                    int type = c.getMessageType();
                                    for (DataTransferListener sl:listener) {
                                        sl.dataReceived(c, type);
                                    }
                                }

                                for (DataTransferListener sl:listener) {
                                    sl.dataReceivedEnd();
                                }

                                synchronized (SYNC) {
                                    for (ClientPreProcessor cpp:pres) {
                                        cpp.afterReceiving();
                                    }
                                }
                            }
                        } else {
                            disconnectInternal();
                        }
                        long end=System.nanoTime();
                        end=(end-start)/1000000L;
                        
                        if (end>250) {
                        	NetLogger.log("Client: Client lags ("+end+"ms)!");
                        }
                        
                        long st=Math.min(NetGlobals.clientWaitTime, Math.max(0,NetGlobals.clientWaitTime-end));
                        if (st>=0) {
                        	try {
                        		Thread.sleep(st);
                        	} catch(Exception e) {
                        		pc.interrupted();
	                        	// This is intentionally
	                        }
                        } else {
                        	Thread.yield();
                        }
                    } else {
                    	if (!disconnect) {
	                    	// This branch is entered only, if the client doesn't return any data to
	                    	// send. This happens almost never...
	                    	waitingForData=true;
	                    	try {
	                    		Thread.sleep(10);
	                    	} catch(Exception e) {
	                        	// This is intentionally
	                        }
	                    	waitingForData=false;
                    	} else {
                    		disconnectInternal();
                    	}
                    }
                }
                is.close();
                os.close();
                conn.close();
                connected = false;
                NetLogger.log("Client: Network thread terminated!");
            } catch (Exception e) {
            	exit=true;
                throw new RuntimeException(e);
            }
        }
    }
}
