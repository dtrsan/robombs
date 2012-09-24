package robombs.game;

/**
 * A simple key/value kind of bean that is used to transfer any kind of info.
 */
public class InfoLine {

    public final static int PLAYER_ADDED=0;
    public final static int PLAYER_REMOVED=1;
    public final static int SCORE_TABLE=2;
    public final static int SYSTEM_OUT=3;
    public final static int MAP_ENTRY=4;
    public final static int READY_YOURSELF=5;
    public final static int ROUND_COMPLETED=6;
    public final static int TEAM_ASSIGNED=7;
    
    public final static int SERVER_STARTED_GAME=1000;
    public final static int ALL_CLIENTS_READY=1001;
    public final static int END_GAME=1002;
    public final static int NEXT_LEVEL=1003;

    private String key=null;
    private String value=null;
    private int type=0;
    private int cnt=0;

    /**
     * Create a new InfoLine.
     * @param type int the type (see constants above)
     * @param cnt int a counter value
     * @param key String the key
     * @param value String the value
     */
    public InfoLine(int type, int cnt, String key, String value) {
        this.key=key;
        this.value=value;
        this.type=type;
        this.cnt=cnt;
    }

    /**
     * Returns the key
     * @return String the key
     */
    public String getKey() {
        return key;
    }

    /**
     * Returns the value
     * @return String the value
     */
    public String getValue() {
        return value;
    }

    /**
     * Returns the type
     * @return int the type
     */
    public int getType() {
        return type;
    }

    /**
     * Returns the counter
     * @return int the counter's value
     */
    public int getCount() {
        return cnt;
    }
}
