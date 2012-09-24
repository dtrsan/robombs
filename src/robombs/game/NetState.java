package robombs.game;

public class NetState {

	public final static int STATE_NOT_CONNECTED=0;
	public final static int STATE_CONNECTED=10;
	public final static int STATE_READY=20;
	public final static int STATE_PREPARING=30;
	public final static int STATE_LEVEL_LOADED=40;
	public final static int STATE_WAITING=50;
	public final static int STATE_RUNNING=60;
	
	private volatile int state=STATE_NOT_CONNECTED;
	
	public void setState(int state) {
		this.state=state;
	}
	
	public void reset() {
		state=STATE_NOT_CONNECTED;
	}
	
	public int getState() {
		return state;
	}
	
}
