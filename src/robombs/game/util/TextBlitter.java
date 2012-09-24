package robombs.game.util;

import com.threed.jpct.*;
import java.awt.*;

/**
 * A class to blit text. Based on class written by CyberKilla.
 */
public class TextBlitter {

	private static GLFont font=null;
	
	static {
		Font f=new Font("Arial", Font.BOLD, 15);
		font=GLFont.getGLFont(f);
	}
	
	public static GLFont create(int size) {
		Font f=new Font("Arial", Font.BOLD, size);
		return GLFont.getGLFont(f);
	}

    public static int getWidth(String s) {
    	return font.getStringBounds(s).width;
    }
    
    public static void blitText(FrameBuffer buffer, String line, int x, int y) {
    	int offset=font.fontHeight/3*2;
        font.blitString(buffer, line, x, y+offset, 12, null);
    }
    
    public static void blitText(FrameBuffer buffer, String line, int x, int y, int maxX, int maxY) {
    	int offset=font.fontHeight/3*2;
    	font.blitString(buffer, line, x, y+offset, 12, null);
    }
    
    public static int getWidth(GLFont font, String s) {
    	return font.getStringBounds(s).width;
    }
    
    public static void blitText(GLFont font, FrameBuffer buffer, String line, int x, int y, Color col) {
    	int offset=font.fontHeight/3*2;
        font.blitString(buffer, line, x, y+offset, 12, col);
    }
}
