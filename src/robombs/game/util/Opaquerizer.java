package robombs.game.util;

import com.threed.jpct.*;

public class Opaquerizer implements ITextureEffect {

	public void init(Texture t) {
	}

	public boolean containsAlpha() {
		return false;
	}

	public void apply(int[] dest, int[] src) {
		for (int i = 0; i < dest.length; i++) {
			int rgb = src[i];
			if ((rgb & 0x0f0f0f) == rgb) {
				rgb = rgb | 0x10;
			}
			dest[i] = rgb;
		}
	}
}
