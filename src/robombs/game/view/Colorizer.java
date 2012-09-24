package robombs.game.view;

import java.util.*;

import robombs.clientserver.NetLogger;
import robombs.game.TeamAssigner;
import robombs.game.model.*;
import robombs.game.util.*;

import com.threed.jpct.*;

public class Colorizer {

	private List<String> textures = new ArrayList<String>();
	private Set<Integer> colored = new HashSet<Integer>();
	private static Colorizer instance = null;

	private Colorizer() {
	}

	public synchronized static Colorizer getInstance() {
		if (instance == null) {
			instance = new Colorizer();
		}
		return instance;
	}

	public synchronized void init() {
		colored.clear();
	}

	public synchronized void unload(FrameBuffer buffer) {
		TextureManager tm = TextureManager.getInstance();
		Collections.reverse(textures); // Reverse the order to remove the last
		// texture first..helps the
		// TextureManager's internals to save
		// some memory
		for (String texture : textures) {
			if (tm.containsTexture(texture)) {
				tm.removeAndUnload(texture, buffer);
			}
		}
		textures.clear();
		colored.clear();
		NetLogger.log("Client: Additional textures unloaded (" + tm.getTextureCount() + ")!");
	}

	public synchronized void colorize(ClientObject co, LocalObject lo) {
		// Assign a new texture to a bot.
		TextureManager tm = TextureManager.getInstance();

		float rf = 1;
		float gf = 1;
		float bf = 1;

		int team = TeamAssigner.getTeam(lo.getClientID(), lo.getObjectID());

		if (team == 0) {

			// Try to color the clients in a fixed manner according to their
			// IDs.
			// For the bot client, this doesn't work. We have to take the
			// objectID
			// into
			// calculation...this doesn't create unique colors over the rounds,
			// but...well...

			int num = lo.getClientID() - 1;

			if ((num & 1) == 1) {
				rf = 1.5f;
			}
			if ((num & 2) == 2) {
				gf = 1.5f;
			}
			if ((num & 4) == 4) {
				bf = 1.5f;
			}
			if ((num & 8) == 8) {
				rf = 0.5f;
				gf = 1.5f;
			}
			if ((num & 16) == 16) {
				gf = 0.5f;
				rf = 1.5f;
			}
			if ((num & 32) == 32) {
				bf = 0.5f;
				gf = 1.5f;
			}
			if ((num & 64) == 64) {
				gf = 0.5f;
				bf = 1.5f;
			}

			if (colored.contains(Integer.valueOf(num))) {
				int no = lo.getObjectID();
				if ((no & 1) == 1) {
					rf += 0.5f;
				}
				if ((no & 2) == 2) {
					rf -= 0.5f;
				}
				if ((no & 4) == 4) {
					gf += 0.5f;
				}
				if ((no & 8) == 8) {
					gf -= 0.5f;
				}
				if ((no & 16) == 16) {
					bf += 0.5f;
				}
				if ((no & 32) == 32) {
					bf -= 0.5f;
				}

			} else {
				colored.add(Integer.valueOf(num));
			}
		} else {
			switch (team) {
			case 1:
				rf=1.6f;
				bf=0.2f;
				gf=0.2f;
				break;
			case 2:
				rf=0.2f;
				bf=1.6f;
				gf=0.2f;
				break;
			case 3:
				rf=0.2f;
				bf=0.2f;
				gf=1.6f;
				break;
			case 4:
				rf=1.6f;
				bf=1.6f;
				gf=1.6f;
				break;
			}
		}

		Texture nt = TextureUtils.colorizeAlienSkin(rf, gf, bf);
		String name = "created_for_bot_" + lo.getClientID() + "/" + lo.getObjectID();
		textures.add(name);
		if (!tm.containsTexture(name)) {
			tm.addTexture(name, nt);
		} else {
			// This should never happen, but just to be sure...
			tm.replaceTexture(name, nt);
		}
		co.setTexture(name);
	}

}
