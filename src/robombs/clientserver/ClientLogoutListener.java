package robombs.clientserver;

/**
 * A listener that will be notified by the server in case of a client's log out.
 */
public interface ClientLogoutListener {
    /**
     * A client has been logged out.
     * @param ci the ClientInfo of the logged out client
     * @return DataContainer an optional DataContainer (may be null) that the server will broadcast to all other clients (usually a "XXX logged out" message or something).
     */
    DataContainer loggedOut(ClientInfo ci);
}
