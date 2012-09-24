package robombs.game;

import robombs.clientserver.*;

/**
 * An info data container that offers support for transfering info messages. This container may contain
 * multiple InfoLines.
 */
public class InfoDataContainer extends DataContainer {

    /**
     * Create a new container.
     */
    public InfoDataContainer() {
        super();
        setMessageType(MessageTypes.IMPORTANT_INFO);
    }

    /**
     * Build a new info data container from the content of a generic data container.
     * @param dc DataContainer the generic data container
     */
    public InfoDataContainer(DataContainer dc) {
        super(dc);
        setClientInfo(dc.getClientInfo());
        setMessageType(MessageTypes.IMPORTANT_INFO);
    }

    /**
     * Adds a new info line to the container.
     * @param line InfoLine the info
     */
    public void add(InfoLine line) {
        add(line.getType());
        add(line.getCount());
        add(line.getKey());
        add(line.getValue());
    }

    /**
     * Gets the next info line from the container.
     * @return InfoLine
     */
    public InfoLine getInfoLine() {
        return new InfoLine(getNextInt(), getNextInt(), getNextString(), getNextString());
    }

}
