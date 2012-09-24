package robombs.game;

import robombs.game.model.*;

import com.threed.jpct.*;

/**
 * An event is everything that the server and the other clients should be notified of but that's not really physical, like
 * a new position of the player would be.
 */
public class Event {

    public static final int BOMB_OVERLOADED = 1;
    public static final int PLAYER_DAMAGE = 2;
    public static final int PLAYER_DEAD = 3;
    public final static int ENTITY_REMOVE=4;
    public final static int FIRE=5;
    public final static int PLAYER_RESPAWNED=6;
    public final static int PLAYER_RESET_HEALTH=7;
    public final static int PLACE_BOMB=8;
    public final static int BOMB_HIT=9;
    public final static int EXPLOSION_HIT=10;
    public final static int CRATE_HIT=11;
    public final static int REMOVE_CRATE=12;
    public final static int LOGIN_REJECTED=14;
    public final static int NEXT_LEVEL_REQUEST=15;
    public final static int ROUND_LOST=16;
    public final static int BOMB_ITEM_COLLECTED=17;
    public final static int FIREPOWER_ITEM_COLLECTED=18;
    public final static int COLLECT_ITEM=19;
    public final static int ONLY_BOTS_LEFT=20;
    public final static int BOMB_DISABLED=21;
    public final static int BOMB_TOUCHED=22;
    public final static int KICK_ITEM_COLLECTED=23;
    public final static int DISEASE_ITEM_COLLECTED=24;
    public final static int TAUNT=25;
    public final static int TEAM_SET=26;
    
    public final static int PLAYER_READY=1000;
    public final static int PLAYER_NOT_READY=1001;
    public final static int LEVEL_LOADED=1002;
    
    private int sourceID = -1;
    private int targetID = -1;
    private int targetClientID = -1;
    private int sourceClientID = -1;
    private int type = 0;
    private int value = 0;
    private SimpleVector origin=SimpleVector.ORIGIN;
    private SimpleVector direction=SimpleVector.ORIGIN;

    /**
     * Creates a new event. This is used for creating bullet hit events.
     * @param type int the type of the event. See the constants above of possible types.
     * @param sourceID int the object id of the source object triggering this event (if any)
     * @param targetID int the id of the target object of this event (if any).
     * @param targetClientID int the id of the target client
     */
    public Event(int type, int sourceID, int targetID, int targetClientID) {
        this.sourceID = sourceID;
        this.targetID = targetID;
        this.targetClientID = targetClientID;
        this.type = type;
    }

    /**
     * Creates a new event.
     * @param type int the type of the event. See the constants above of possible types.
     * @param source LocalObject the source object of the event
     * @param target LocalObject the target object of the event
     */
    public Event(int type, LocalObject source, LocalObject target) {
        this(type, source!=null?source.getObjectID():-99, target.getObjectID(), target.getClientID());
        if (source!=null) {
        	setSourceClientID(source.getClientID());
        }
        else {
        	setSourceClientID(-99);
        }
    }
    
    /**
     * Sets the origin of the event. Not all events have this.
     * @param org SimpleVector the origin
     */
    public void setOrigin(SimpleVector org) {
        origin=org;
    }
    
    public void setDirection(SimpleVector dir) {
    	direction=dir;
    }

    /**
     * Gets the origin of th event. Not all events have this.
     * @return SimpleVector the origin
     */
    public SimpleVector getOrigin() {
        return origin;
    }
    
    public SimpleVector getDirection() {
    	return direction;
    }

    /**
     * Sets a value for the event. Not all events have this.
     * @param val int the value
     */
    public void setValue(int val) {
        this.value = val;
    }

    /**
     * Returns the value for the event. Not all events have this.
     * @return int the value
     */
    public int getValue() {
        return value;
    }

    /**
     * Returns the type of the event.
     * @return int the type
     */
    public int getType() {
        return type;
    }

    /**
     * Sets the id of the source client.
     * @param id int the id
     */
    public void setSourceClientID(int id) {
        this.sourceClientID = id;
    }

    /**
     * Returns the id of the source client.
     * @return int the id
     */
    public int getSourceClientID() {
        return sourceClientID;
    }

    /**
     * Returns the id of the source object.
     * @return int the id
     */
    public int getSourceID() {
        return sourceID;
    }

    /**
     * Returns the id of the target object.
     * @return int the id
     */
    public int getTargetID() {
        return targetID;
    }

    /**
     * Returns the id of the target client
     * @return int the if
     */
    public int getTargetClientID() {
        return targetClientID;
    }

    public String toString() {
        return this.getType() + "/" + this.getSourceID() + "/" + this.getSourceClientID() + "/" + this.getTargetID() + "/" + this.getTargetClientID();
    }
}
