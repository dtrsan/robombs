package robombs.clientserver;

/**
 * A DataTransferListener can be added to a client or to a server. It will be notified if data has been received.
 */
public interface DataTransferListener {

    /**
     * Data has been received by the server/client to which this listener has been added. This method will be called for
     * each DataContainer. A transfer can consist of many DataContainers.
     * @param data the DataContainer that has been received.
     * @param messageType the type of the message like MessageType defines it
     * @return DataContainer[] an optional result. The client doesn't care for this result. The server will send it back to
     * the client from which is has received the data.
     */
    DataContainer[] dataReceived(DataContainer data, int messageType);

    /**
     * The server/client has finished receiving in this transfer. This method will be called once after receiving all
     * data, while the one above can be called multiple times while receiving.
     */
    void dataReceivedEnd();
}
