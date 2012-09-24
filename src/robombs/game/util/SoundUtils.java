package robombs.game.util;

import robombs.game.sound.*;

public class SoundUtils {

	public static void loadSounds() {
		SoundManager sm=SoundManager.getInstance();
		sm.addSound("explosion", "data/sounds/explosion.wav");
		sm.addSound("bounce", "data/sounds/bounce.wav");
		sm.addSound("bubble", "data/sounds/bubble.wav");
		sm.addSound("gun", "data/sounds/gun.wav");
		sm.addSound("happykids", "data/sounds/happykids.wav");
		sm.addSound("scream", "data/sounds/scream.wav");
		sm.addSound("boo", "data/sounds/sfxboo.wav");
		sm.addSound("drop", "data/sounds/drop.wav");
		sm.addSound("comeon", "data/sounds/comeon.wav");
		sm.addSound("click", "data/sounds/click.wav");
		sm.addSound("water", "data/sounds/water.wav");
		sm.addSound("splash", "data/sounds/splash.wav");
		sm.addSound("woohoo", "data/sounds/woohoo.wav");
		sm.addSound("cough", "data/sounds/cough.wav");
		
		sm.addSound("taunt0","data/sounds/taunt/yes-hahahaa.wav");
		sm.addSound("taunt1","data/sounds/taunt/unbelievable.wav");
		sm.addSound("taunt2","data/sounds/taunt/maybe-next-time-huh.wav");
		sm.addSound("taunt3","data/sounds/taunt/nice-work.wav");
		sm.addSound("taunt4","data/sounds/taunt/im-sorry.wav");
		sm.addSound("taunt5","data/sounds/taunt/im-in-touble.wav");
		sm.addSound("taunt6","data/sounds/taunt/hot-stuff-coming-up.wav");
		sm.addSound("taunt7","data/sounds/taunt/here-i-come.wav");
		sm.addSound("taunt8","data/sounds/taunt/do-you-like-it.wav");
		sm.addSound("taunt9","data/sounds/taunt/dinner-is-served.wav");
	}
}
