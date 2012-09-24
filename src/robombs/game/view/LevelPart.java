package robombs.game.view;

import robombs.game.*;

import com.threed.jpct.Object3D;

public class LevelPart extends Object3D {

	static final private long serialVersionUID=1L;
	
	public LevelPart(Object3D obj) {
		super(obj);
		if (Globals.compiledObjects) {
			compile();
		}
	}

	public LevelPart(int max) {
		super(max);
		if (Globals.compiledObjects) {
			compile();
		}
	}
}
