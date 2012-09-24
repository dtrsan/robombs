package robombs.clientserver;

import java.net.*;
import java.util.*;

/**
 * A server side wrapper that identifies a connected client.
 */
public class ClientInfo {

    /**
     * A time out value after which a client is considered to be lost.
     */
    public final static long TIMEOUT = 20 * 1000;

    private static Set<Integer> usedIDs=new HashSet<Integer>();
    
    private InetAddress addr = null;
    private int port = 0;
    private long lastCall = 0;
    private int id = 0;
    private boolean wantsZips = false;
    private boolean loggedOut = false;

    /**
     * Create a new ClientInfo.
     * @param addr the client's address
     * @param port the client's port
     */
    public ClientInfo(InetAddress addr, int port) {
        this.addr = addr;
        this.port = port;
        id = getFreeID();
        touch();
    }
    
    private synchronized int getFreeID() {
    	int i=1;
    	while (usedIDs.contains(i)) {
    		i++;
    	}
    	usedIDs.add(i);
    	return i;
    }

    /**
     * Sets if the client uses zip mode or not.
     * @param mode does it?
     */
    public void setZipMode(boolean mode) {
        wantsZips = mode;
    }

    /**
     * Returns if the client uses zip mode or not.
     * @return boolean does it?
     */
    public boolean getZipMode() {
        return wantsZips;
    }

    /**
     * Returns the client's ID. The ID is unique per server.
     * @return int the ID
     */
    public int getID() {
        return id;
    }

    /**
     * "Touches" the client, i.e. make sure that it's not getting "lost". This not all not based on actual
     * client state but on time only.
     */
    public void touch() {
        lastCall = NetTime.getTime();
    }

    /**
     * Is the client lost?
     * @return boolean is it?
     */
    public boolean isLost() {
        return NetTime.getTime() - lastCall > TIMEOUT;
    }

    /**
     * Returns the client's address.
     * @return InetAddress the address
     */
    public InetAddress getAddress() {
        return addr;
    }

    /**
     * Returns the client's port.
     * @return int the port
     */
    public int getPort() {
        return port;
    }

    /**
     * Indicates that the client has been logged out either by its own request or by the server.
     * @return boolean logged out?
     */
    public synchronized boolean isLoggedOut() {
        return loggedOut;
    }

    /**
     * Marks the client as logged out.
     */
    public synchronized void logout() {
        loggedOut = true;
        usedIDs.remove(id);
    }

    public String toString() {
        return addr + ":" + port;
    }

    public boolean equals(Object obj) {
        if (obj instanceof ClientInfo) {
            ClientInfo ci = (ClientInfo) obj;
            return addr.equals(ci.getAddress()) && port == ci.getPort();
        }
        return false;
    }

    public int hashCode() {
        return addr.hashCode() + port;
    }
}
