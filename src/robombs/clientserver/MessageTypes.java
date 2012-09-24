package robombs.clientserver;

/**
 * An interface that defines some constants for client and server. These values are part of each transfered data block.
 */
public interface MessageTypes {

    /**
     * A login request from the client
     */
    public final static int LOGIN_REQUEST = 9999;

    /**
     * A message from the server to the client that the login was a success
     */
    public final static int LOGIN_SUCCESS = 9998;

    /**
     * A message from the server to the client that the login has failed
     */
    public final static int LOGIN_FAILURE = 9997;

    /**
     * A logout request from the client
     */
    public final static int LOGOUT_REQUEST = 8999;

    /**
     * A message from the server to the client that it has been logged out correctly
     */
    public final static int LOGOUT_SUCCESS = 8998;

    /**
     * This is a hack...it's used to numerically seperate the above constants from the lower ones.
     */
    public final static int INTERNAL_BORDER = 1000;

    /**
     * The message is a simple data transfer. This is the case for most messages.
     */
    public final static int OBJ_TRANSFER = 0;

    /**
     * The message is an info (and an important one...as if there were others...). An info is something like
     * "player added" or the scoring board.
     */
    public final static int IMPORTANT_INFO = 3;

    /**
     * The message is an event. An event is something of which a client (or server) has be informed but that
     * doesn't affect the state of a local object directly. Examples are a player firing the gun, so that the
     * clients have to play a sound.
     */
    public final static int EVENT = 4;
}
