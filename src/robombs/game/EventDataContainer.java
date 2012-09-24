package robombs.game;

import robombs.clientserver.*;

import com.threed.jpct.*;

/**
 * An extended data container for transfering events.
 */
public class EventDataContainer extends DataContainer {

    /**
     * Create a new container.
     */
    public EventDataContainer() {
        super();
        setMessageType(MessageTypes.EVENT);
    }

    /**
     * Build a new event data container from the content of a generic data container.
     * @param dc DataContainer the generic data container
     */
    public EventDataContainer(DataContainer dc) {
        super(dc);
        setClientInfo(dc.getClientInfo());
        setMessageType(MessageTypes.EVENT);
    }

    /**
     * Adds an event to the container.
     * @param event Event the event to add
     */
    public void add(Event event) {
        add(event.getType());
        add(event.getSourceID());
        add(event.getTargetID());
        add(event.getTargetClientID());
        add(event.getSourceClientID());
        add(event.getValue());
        SimpleVector s = event.getOrigin();
        add(s.x);
        add(s.y);
        add(s.z);
        s = event.getDirection();
        add(s.x);
        add(s.y);
        add(s.z);
    }

    /**
     * Returns the next event from the container. A container may contain multiple events.
     * @return Event
     */
    public Event getEvent() {
        Event ev = new Event(getNextInt(), getNextInt(), getNextInt(), getNextInt());
        ev.setSourceClientID(getNextInt());
        ev.setValue(getNextInt());
        ev.setOrigin(new SimpleVector(getNextFloat(), getNextFloat(), getNextFloat()));
        ev.setDirection(new SimpleVector(getNextFloat(), getNextFloat(), getNextFloat()));
        return ev;
    }
}
