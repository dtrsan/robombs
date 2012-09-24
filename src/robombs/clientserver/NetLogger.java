package robombs.clientserver;

/**
 * An incredible complex logging class. One may add log4j here or whatever one prefers.
 * or leave it like it is (which is what i prefer...).
 */
public class NetLogger {

  public static void log(String txt) {
	  System.out.println("["+System.currentTimeMillis()+"] - "+txt);
  }
}
