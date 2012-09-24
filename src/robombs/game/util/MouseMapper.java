package robombs.game.util;

import org.lwjgl.input.*;
import com.threed.jpct.*;

/**
 * A helper class similar to the KeyMapper-class from the jPCT-API that simplyfies mouse handling.
 */
public class MouseMapper {

    private boolean hidden = false;
    private int height = 0;

    /**
     * Create a new mapper.
     * @param buffer the current frame buffer
     */
    public MouseMapper(FrameBuffer buffer) {
        height = buffer.getOutputHeight();
        init();
    }

    /**
     * Hide the mouse.
     */
    public void hide() {
        if (!hidden) {
            Mouse.setGrabbed(true);
            hidden = true;
        }
    }

    /**
     * Show the mouse.
     */
    public void show() {
        if (hidden) {
            Mouse.setGrabbed(false);
            hidden = false;
        }
    }

    /**
     * Is the mouse visible (i.e. not hidden)?
     * @return boolean is it?
     */
    public boolean isVisible() {
        return!hidden;
    }

    /**
     * Destroy the mapper and the mouse (virtually...:-))
     */
    public void destroy() {
        show();
        if (Mouse.isCreated()) {
            Mouse.destroy();
        }
    }

    /**
     * If a mouse button down?
     * @param button the button's number
     * @return boolean is it down?
     */
    public boolean buttonDown(int button) {
        return Mouse.isButtonDown(button);
    }
    
    public int getWheel() {
    	int d=Mouse.getDWheel();
    	if (d==0) {
    		return 0;
    	}
    	if (d<0) {
    		return -1;
    	}
    	return 1;
    }

    /**
     * Gets the x-coordinate of the mouse.
     * @return the x-coordinate
     */
    public int getMouseX() {
        return Mouse.getX();
    }

    /**
     * Gets the y-coordinate of the mouse.
     * @return the y-coordinate
     */
    public int getMouseY() {
        return height - Mouse.getY();
    }

    /**
     * Gets the delta in x-direction since the last call.
     * @return int the delta
     */
    public int getDeltaX() {
        if (Mouse.isGrabbed()) {
            return Mouse.getDX();
        } else {
            return 0;
        }
    }

    /**
     * Gets the delta in y-direction since the last call.
     * @return int the delta
     */
    public int getDeltaY() {
        if (Mouse.isGrabbed()) {
            return Mouse.getDY();
        } else {
            return 0;
        }
    }

    private void init() {
        try {
            if (!Mouse.isCreated()) {
                Mouse.create();
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
