package robombs.clientserver;

import java.net.*;

/**
 * A ServerEntry is a simple wrapper class for server related information. It's mainly used by the ServerBrowser.
 */
public class ServerEntry implements Comparable<ServerEntry> {

    /**
     * Default timeout for this entry. If the entry hasn't been touched for this long, it will be regarded as old.
     */
    public final int TIMEOUT = 10000;

    private String name = null;
    private InetAddress addr = null;
    private int port = 0;
    private int clients = 0;
    private long lastFound = 0;
    private boolean connected = false;

    /**
     * Creates a new ServerEntry
     * @param name the name of the server. This is not the address but any name like "Peter's server" or something.
     * @param addr the address of the server
     * @param port the port of the server
     * @param clients the number of clients currently connected to that server
     */
    public ServerEntry(String name, InetAddress addr, int port, int clients) {
        lastFound = NetTime.getTime();
        this.addr = addr;
        this.name = name;
        this.port = port;
        this.clients = clients;
    }

    /**
     * Returns the name of the server.
     * @return String the name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the server's address.
     * @return InetAddress the address
     */
    public InetAddress getAddress() {
        return addr;
    }

    /**
     * Sets this entry to "connected".
     * @param con boolean do we have an connection to this server or not?
     */
    public void setConnected(boolean con) {
        connected = con;
    }

    /**
     * Do we have an connection to the server of this entry?
     * @return boolean do we?
     */
    public boolean isConnected() {
        return connected;
    }

    /**
     * Returns the port on which the server is running.
     * @return int the port
     */
    public int getPort() {
        return port;
    }

    /**
     * Returns the number of clients that the server has.
     * @return int the number of clients
     */
    public int getClientCount() {
        return clients;
    }

    /**
     * Sets the number of clients.
     * @param cnt the client count
     */
    public void setClientCount(int cnt) {
        clients = cnt;
    }

    /**
     * "Touches" this entry, i.e. isOld will return false after doing this.
     */
    public void touch() {
        lastFound = NetTime.getTime();
    }

    /**
     * Is the server entry "old"?
     * @return boolean is it?
     */
    public boolean isOld() {
        return NetTime.getTime() - lastFound > TIMEOUT;
    }

    public boolean equals(Object obj) {
        if (obj instanceof ServerEntry) {
            ServerEntry se = (ServerEntry) obj;
            return name.equals(se.getName()) && addr.equals(se.getAddress()) && port == se.getPort();
        }
        return false;
    }

    public int hashCode() {
        return name.hashCode() + addr.hashCode() + port;
    }

    public int compareTo(ServerEntry obj) {
        ServerEntry se = (ServerEntry) obj;
        return name.compareTo(se.getName());
    }
}
