package robombs.game.gui;

import robombs.game.util.*;

import java.awt.*;

import com.threed.jpct.*;
import com.threed.jpct.util.*;

/**
 * A simple label component for displaying text in the GUI.
 */
public class Label extends GUIComponent {
    private String label = "";
    private int xp = 0;
    private int yp = 0;
    private GLFont font=null;
    private Color col=null;

    /**
     * Creates a new label at the given position (top left corner).
     * @param xpos the x-position
     * @param ypos the y-position
     */
    public Label(int xpos, int ypos) {
        this.xp = xpos;
        this.yp = ypos;
    }

    /**
     * Sets the text of the label.
     * @param text the text
     */
    public void setText(String text) {
        this.label = text;
    }
    
    public void setColor(Color col) {
    	this.col=col;
    }
    
    public void setFont(GLFont font) {
    	this.font=font;
    }

    public boolean evaluateInput(MouseMapper mouse, KeyMapper keyMapper) {
        return super.evaluateInput(mouse, keyMapper);
    }

    public void draw(FrameBuffer buffer) {
        if (visible) {
        	if (font==null) {
        		TextBlitter.blitText(buffer, label, getParentX() + xp, getParentY() + yp);
        	} else {
        		TextBlitter.blitText(font, buffer, label, getParentX() + xp, getParentY() + yp, col);
        	}
            super.draw(buffer);
        }
    }
}
