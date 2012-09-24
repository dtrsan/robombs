package robombs.game.gui;

import com.threed.jpct.util.*;
import java.awt.event.*;

import robombs.game.util.*;

import com.threed.jpct.*;

/**
 * A text field component. After clicking into a text field, you'll get a cursor and can type some stuff into it.
 */
public class TextField extends GUIComponent {
    private String content = "";
    private int xp = 0;
    private int yp = 0;
    private int xs = 0;
    private int ys = 0;
    private boolean active = false;

    /**
     * Creates a new text field.
     * @param xpos the x-position of the upper left corner.
     * @param ypos the y-position of the upper left corner.
     * @param xdim the dimension in x direction.
     * @param ydim the dimension in y direction.
     */
    public TextField(int xpos, int ypos, int xdim, int ydim) {
        this.xp = xpos;
        this.yp = ypos;
        this.xs = xdim;
        this.ys = ydim;
    }

    /**
     * The current text of the text field.
     * @return String the text
     */
    public String getText() {
        return content;
    }

    /**
     * Sets the text for the text field. Any new text typed by the user will be appended to this text.
     * @param txt the new text
     */
    public void setText(String txt) {
        content = txt;
    }

    public boolean evaluateInput(MouseMapper mouse, KeyMapper mapper) {
        boolean has = super.evaluateInput(mouse, mapper);
        if (!has && isVisible()) {
            boolean input = false;

            int xpos = getParentX();
            int ypos = getParentY();

            int x = mouse.getMouseX() - xpos;
            int y = mouse.getMouseY() - ypos;
            if (mouse.buttonDown(0) || active) {
                if (x >= xp && x <= xp + xs && y >= yp && y <= yp + ys) {
                    input = true;
                    active = true;
                } else {
                    if (mouse.buttonDown(0)) {
                        active = false;
                    } else {
                        input = true;
                    }
                }
            }
            if (input) {
                KeyState ks = null;
                while ((ks = mapper.poll()) != KeyState.NONE) {
                    if (ks.getState()) {
                        int code = ks.getKeyCode();
                        char chr = ks.getChar();
                        if (content.length() < (xs - 9) / 9) {
                            if ((chr >= '0' && chr <= '9') ||
                                (chr >= 'a' && chr <= 'z') ||
                                (chr >= 'A' && chr <= 'Z') || chr == ':' || chr == '.') {
                                content += chr;
                            }
                        }
                        if (code == KeyEvent.VK_BACK_SPACE || code == KeyEvent.VK_DELETE) {
                            content = content.substring(0, Math.max(0, content.length() - 1));
                        }
                    }
                }
            }
            setText(content);
            return input;
        } else {
            return has;
        }
    }

    public void draw(FrameBuffer buffer) {
        if (visible) {
            String txt = content;
            if (active) {
                txt += "|";
            }
            TextBlitter.blitText(buffer, txt, getParentX() + xp, getParentY() + yp);
            super.draw(buffer);
        }
    }
}
