package robombs.game.model;

/**
 * A simple wrapper class for a player's current state, i.e. the name and the number of frags/deaths.
 */
public class PlayerInfo implements Comparable<PlayerInfo> {

    private String name=null;
    private int frags=0;
    private int killed=0;
    private int lost=0;
    private int won=0;
    private int cid=-1;
    private boolean isReady=false;
    private boolean isBot=false;
    private boolean isDead=false;
    private int objID=0;

    /**
     * Create a new player info
     * @param name the player's name
     */
    public PlayerInfo(String name, int cid, int oid) {
        this.name=name;
        this.cid=cid;
        objID=oid;
    }
    
    public int getObjectID() {
    	return objID;
    }
    
    public boolean isBot() {
    	return isBot;
    }
    
    public void setBot(boolean isBot) {
    	this.isBot=isBot;
    }

    public int compareTo(PlayerInfo obj) {
    	return getName().toLowerCase().compareTo(obj.getName().toLowerCase());
    }
    
    public boolean isReady() {
    	return isReady;
    }
    
    public void ready(boolean isReady) {
    	this.isReady=isReady;
    }
    
    /**
     * Returns the player's name.
     * @return String the name
     */
    public String getName() {
        return name;
    }
    
    public int getClientID() {
    	return cid;
    }

    /**
     * Resets the player's info.
     */
    public void reset() {
        frags=0;
        killed=0;
        isDead=false;
    }
    
    public void revive() {
    	isDead=false;
    }

    public void kill() {
    	isDead=true;
    }
    
    public boolean isDead() {
    	return isDead;
    }
    
    public void lost() {
    	lost++;
    }
    
    public void won() {
    	won++;
    }
    
    public int getWins() {
    	return won;
    }
    
    public int getLoses() {
    	return lost;
    }
    
    /**
     * Increment the number of frags.
     */
    public void incFrags() {
        frags++;
    }

    /**
     * Increment the number of deaths.
     */
    public void incKilled() {
        killed++;
    }

    /**
     * Gets the number of frags.
     * @return int the number of frags
     */
    public int getFrags() {
        return frags;
    }

    /**
     * Gets the number of deaths.
     * @return int the number of deaths
     */
    public int getKilled() {
        return killed;
    }
}
