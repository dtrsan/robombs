package robombs.game;

import java.util.*;

/**
 * This is a class used by the client to store events that should be transfered to the server in the next transfer.
 * An event can be death, firing,...<br>
 * The queue can contain events for different clients running in one VM.
 */
public class ClientEventQueue {

    private List<Event> events=new ArrayList<Event>();
    private Set<Integer> collectedItems=new HashSet<Integer>();

    /**
     * Creates a new queue.
     */
    public ClientEventQueue() {
    }
    
    /**
     * Adds an event to the queue.
     * @param event Event the event to add
     */
    public synchronized void add(Event event) {
        events.add(event);
    }

    /**
     * Clears the queue.
     */
    public synchronized void clear() {
        events.clear();
        collectedItems.clear();
    }
    
    /**
     * Locally processes the events that the local client can execute without the server's confirmation. This is
     * a shortcut in event processing that improves the feel on slower network connection. It's not really amazing though...
     * @param ep
     */
    public synchronized void processLocalEvents(EventProcessor ep, int clientID) {
    	 for(Event event: events) {
    		 Event eve=null;
    		 switch (event.getType()) {
    	        case Event.CRATE_HIT:
    	        	eve = new Event(Event.REMOVE_CRATE, -99, event.getTargetID(), -99);
    	        	eve.setOrigin(event.getOrigin());
    	            break;
    	        case Event.BOMB_ITEM_COLLECTED:
    	        case Event.FIREPOWER_ITEM_COLLECTED:
    	        case Event.KICK_ITEM_COLLECTED:
    	        case Event.DISEASE_ITEM_COLLECTED:
    	        	if (!collectedItems.contains(event.getTargetID())) {
		    	    	eve = new Event(Event.COLLECT_ITEM, event.getSourceID(), event.getTargetID(), -99);
		    	    	eve.setSourceClientID(event.getSourceClientID());
		    	    	eve.setOrigin(event.getOrigin());
		    	    	collectedItems.add(event.getTargetID());
    	        	}
    	        	break;
    		 }
    		 if (eve!=null) {
    			 ep.processEvent(eve);
    		 }
         }
    }

    /**
     * Gets all events scheduled by a client.
     * @param clientID int the ID of the client
     * @return EventDataContainer the events packed into a data container
     */
    public synchronized EventDataContainer getEvents(int clientID) {
        EventDataContainer edc=new EventDataContainer();
        for(Event event: events) {
            event.setSourceClientID(clientID); //??? WTF???
            edc.add(event);
        }
        return edc;
    }
}
