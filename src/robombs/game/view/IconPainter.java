package robombs.game.view;

import robombs.game.model.*;
import com.threed.jpct.*;

public class IconPainter {

	public void paintIcons(LocalPlayerObject lpo, FrameBuffer fb) {
		if (lpo!=null) {
			PlayerPowers pp=lpo.getPlayerPowers();
			paint(45, 5, (int)lpo.getSpecialValue(), fb);
			paint(23, 40, pp.getBombCount(), fb);
			paint(0, 75,  pp.getFirePower()/10, fb);
			int y=110;
			if (pp.canKick()) {
				paint(69,y, 1, fb);
				y+=35;
			}
			if (pp.isSick()!=PlayerPowers.NOT_SICK) {
				paint(93,y,1,fb);
			}
			paintWater(fb, pp.getWater(), pp.getMaxWater());
		}
	}
	
	private void paintWater(FrameBuffer fb, int value, int max) {
		Texture box=TextureManager.getInstance().getTexture("barbox");
		fb.blit(box,0,0, fb.getOutputWidth()-5-64, fb.getOutputHeight()-5-16, 64, 16, true);
		
		box=TextureManager.getInstance().getTexture("bar");
		java.awt.Color col=null;
		if (value!=max) {
			col=java.awt.Color.RED;
		}
		fb.blit(box, 1, 0, fb.getOutputWidth()-5-62, fb.getOutputHeight()-5-14, 14, 16, (int)(60f*(float)value/(float)max), 12, 12, false, col); 
	}
	
	private void paint(int startX, int y,  int cnt, FrameBuffer fb) {
		int width=fb.getOutputWidth();
		int x=width-5-cnt*25;
		Texture t=TextureManager.getInstance().getTexture("icons");
		for (int i=0; i<cnt; i++) {
			fb.blit(t, startX, 0, x, y, 23, 32, true);
			x+=25;
		}
	}
}
