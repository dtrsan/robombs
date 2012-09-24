package robombs.clientserver;

/**
 * A client preprocessor will be called by the SimpleClient as the method names indicate.
 */
public interface ClientPreProcessor {

    /**
     * This method will be called before sending data. It is usually the place, where the actual client implementation
     * prepares/sets the data for the client to send in the coming transfer attempt.
     */
    void beforeSending();

    /**
     * This is called after sending and before receiving the response from the server. It's totally up to the implementation
     * what (if anything) to do here.
     */
    void beforeReceiving();

    /**
     * This is called after receiving the response from the server. It's totally up to the implementation
     * what (if anything) to do here.
     */
    void afterReceiving();
}
