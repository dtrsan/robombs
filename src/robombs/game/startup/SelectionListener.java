package robombs.game.startup;

import com.threed.jpct.*;

public interface SelectionListener {
	void selected(VideoMode vm, boolean fs, int sq, boolean filter, int aa, int ms) throws Exception;
}
