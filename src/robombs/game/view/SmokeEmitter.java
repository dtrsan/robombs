package robombs.game.view;

import com.threed.jpct.*;
import robombs.game.util.*;

public class SmokeEmitter {

	private long maxTime = 0;
	private int freq = 0;
	private SimpleVector pos = null;
	private float startSize;
	private long startTime = 0;
	private long tickCnt = 0;

	public SmokeEmitter(SimpleVector pos, long maxTime, int freq, float startSize) {
		this.maxTime = maxTime;
		this.pos = new SimpleVector(pos);
		this.freq = freq;
		this.startSize = startSize;
		startTime = Ticker.getTime();
	}

	public boolean isOld() {
		return Ticker.hasPassed(startTime, maxTime);
	}

	public void smoke(long ticks, SmokeCloudManager man) {
		if (!isOld()) {
			tickCnt += ticks;
			if (tickCnt >= freq) {
				for (int i = 0; i < tickCnt / freq; i++) {
					SimpleVector vel = new SimpleVector((float) Math.random() / 5f, -0.6 + ((float) Math.random() / 7f), (float) Math.random() / 5f);
					man.addSmokeCloud(pos, vel, maxTime * 3, "smoke", startSize);
				}
				tickCnt = 0;
			}
		}
	}

}
