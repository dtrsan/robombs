package robombs.clientserver;

/**
 * A listener that will be notified by the server in case of a client's log in.
 */
public interface ClientLoginListener {
    /**
     * A client has been logged in.
     * @param ci the ClientInfo of the logged in client
     * @return DataContainer an optional DataContainer (may be null) that the server will broadcast to all other clients (usually a "XXX logged in" message or something).
     */
    DataContainer loggedIn(ClientInfo ci, DataContainer dc);
}
