package robombs.game.startup;

import robombs.game.*;

public class StartUp {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		if (args.length>0) {
			if (args[0].equals("compile")) {
				Globals.compiledObjects=true;
			}
		}
		new ResolutionFrame(new BlueThunderClient());
	}

}
