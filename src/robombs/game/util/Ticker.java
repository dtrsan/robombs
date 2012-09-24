package robombs.game.util;

import robombs.game.Globals;

/**
 * A simple ticker class for measuring in-game-time. This was formerly based on the LWJGL Timer class, but that class
 * has some unfixed problems on some machines/VMs...so it's based on Java5's nanotime now.
 */
public class Ticker {

	private int rate;
    private long s2;
    private static org.lwjgl.util.Timer timer=null;

    static {
    	if (Globals.useLWJGLTimer && timer==null) {
        	timer=new org.lwjgl.util.Timer();
        }
    }
    
    public static long getTime() {
    	if (!Globals.useLWJGLTimer) {
    		return System.nanoTime()/1000000L;
    	}
    	org.lwjgl.util.Timer.tick();
    	return (long)(timer.getTime()*1000f);
    }
    
    /**
     * Create a new ticker that ticks every n-th millisecond.
     * @param tickrateMS the interval
     */
    public Ticker(int tickrateMS) {
        rate = tickrateMS;
        s2 = Ticker.getTime();
    }

    /**
     * Reset the ticker.
     */
    public void reset() {
        s2=Ticker.getTime();
    }

    /**
     * Forward the ticker to the next tick.
     */
    public void forward() {
        s2=Ticker.getTime()-rate;
    }
    
    public static boolean hasPassed(long startTime, long time) {
    	 long dif=Ticker.getTime()-startTime; 
         if (dif<0) { 
                 return true;    
         } 
         return dif>time;
    }
    
    public static boolean hasNotPassed(long startTime, long time) {
   	 long dif=Ticker.getTime()-startTime; 
        if (dif<0) { 
                return true;    
        } 
        return dif<time;
   }

    /**
     * How many ticks have passed since the last call?
     * @return int number of ticks
     */
    public int getTicks() {
        long i = Ticker.getTime();
        if (i - s2 > rate) {
            int ticks = (int) ((i - s2) / (long) rate);
            s2 += (long)rate * ticks;
            return ticks;
        }
        return 0;
    }
}
