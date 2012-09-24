package robombs.clientserver;

/**
 * A Listener solely used in the ServerBrowser to notify in case of changes in the browser's server list.
 */
public interface DataChangeListener {

    /**
     * Data has been changed...
     * @param data the new data
     */
    void dataChanged(Object data);
}
