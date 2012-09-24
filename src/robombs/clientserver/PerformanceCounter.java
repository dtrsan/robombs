package robombs.clientserver;

/**
 * A simple class used by both, the client and the server, to measure bandwidth usage.
 */
public class PerformanceCounter {

    private long time = 0;
    private long bytesIn = 0;
    private long bytesOut = 0;
    private long bpsIn = 0;
    private long bpsOut = 0;
    private long interrupts=0;
    private String label = "";

    /**
     * Creates a new counter. The counter has label that is used in the printStats-method...the label
     * can be anything you like.
     * @param label the label
     */
    public PerformanceCounter(String label) {
        this.label = label;
    }

    /**
     * Prints the stats if more than 10 seconds have elapsed since the last printing.
     */
    public void printStats() {
        if (NetTime.getTime() - time >= 10000) {
            time = NetTime.getTime();
            bpsIn = bytesIn / 10;
            bpsOut = bytesOut / 10;
            NetLogger.log(label + ": In-> " + bpsIn + "bps / Out-> " + bpsOut + "bps  -  interrupts: "+interrupts/10);
            bytesIn = 0;
            bytesOut = 0;
            interrupts=0;
        }
    }

    /**
     * Adds a number of bytes to the in-counter.
     * @param cnt the number
     */
    public void in(int cnt) {
        bytesIn += cnt;
    }
    
    public void interrupted() {
    	interrupts++;
    }

    /**
     * Adds a number of bytes to the out-counter.
     * @param cnt the number
     */
    public void out(int cnt) {
        bytesOut += cnt;
    }
}
