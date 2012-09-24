package robombs.game.gui;

import java.util.*;

import robombs.game.util.*;

import com.threed.jpct.*;
import com.threed.jpct.util.*;

/**
 * GUIComponent is the abstract base class of a very very simple GUI system. The
 * GUI components can't even paint themselves. They are just click and type
 * sensitive areas of the screen. But it shouldn't be too hard to add
 * functionality that improves the situation if you need that.
 */
public abstract class GUIComponent {

	protected int xpos = 0;
	protected int ypos = 0;
	private GUIComponent superC = null;
	private List<GUIComponent> comps = new ArrayList<GUIComponent>();
	protected boolean visible = true;

	/**
	 * Sets the x-position of the component in screen coordinates.
	 * 
	 * @param x
	 *            the x-position
	 */
	public void setX(int x) {
		xpos = x;
	}

	/**
	 * Sets the y-position of the component in screen coordinates.
	 * 
	 * @param y
	 *            the y-position
	 */
	public void setY(int y) {
		ypos = y;
	}

	/**
	 * Sets if the component is visible or hidden. A hidden component must not
	 * react on clicks or key press events.
	 * 
	 * @param visi
	 *            true if visible, false otherwise
	 */
	public void setVisible(boolean visi) {
		visible = visi;
	}

	/**
	 * Is this component visible?
	 * 
	 * @return boolean yes or no...
	 */
	public boolean isVisible() {
		return visible;
	}

	/**
	 * Returns the x-position of the component in screen coordinates.
	 * 
	 * @return int the x-position
	 */
	public int getX() {
		return xpos;
	}

	/**
	 * Returns the y-position of the component in screen coordinates.
	 * 
	 * @return int the y-position
	 */
	public int getY() {
		return ypos;
	}

	/**
	 * If this component is the child of another one, this will return the
	 * parent's x-position. If it isn't, it will return 0.
	 * 
	 * @return int the parent's x-position
	 */
	public int getParentX() {
		if (superC != null) {
			return superC.getX();
		}
		return 0;
	}

	/**
	 * If this component is the child of another one, this will return the
	 * parent's y-position. If it isn't, it will return 0.
	 * 
	 * @return int the parent's y-position
	 */
	public int getParentY() {
		if (superC != null) {
			return superC.getY();
		}
		return 0;
	}

	/**
	 * Returns the parent of this component.
	 * 
	 * @return GUIComponent The parent or null in case that there isn't any
	 */
	public GUIComponent getParent() {
		return superC;
	}

	/**
	 * Adds a child component to this component. A child will be
	 * rendered/processed in the context of and relative to the parent. An
	 * example are buttons (=childs) displayed in a window (=parent).
	 * 
	 * @param c
	 *            the child component
	 */
	public void add(GUIComponent c) {
		comps.add(0, c);
		c.superC = this;
	}

	public void remove(GUIComponent c) {
		comps.remove(c);
		c.superC = null;
	}

	/**
	 * Processes key and mouse events in the context of this component. It then
	 * continues processing on the child components until the event got comsumed
	 * by one or the last child has been processed.
	 * 
	 * @param mouse
	 *            the MouseMapper used to process mouse events
	 * @param keyMapper
	 *            the KeyMapper used to process key events
	 * @return boolean true, if an event occured in the context of this
	 *         component or one of its child components.
	 */
	public boolean evaluateInput(MouseMapper mouse, KeyMapper keyMapper) {
		if (isVisible()) {
			for (GUIComponent c : comps) {
				boolean has = c.evaluateInput(mouse, keyMapper);
				if (has) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Draws this component and all of its childs. Drawing in this "toolkit" is
	 * limited to label and text rendering. Rendering the actual component isn't
	 * done by this toolkit, but can be added easily.
	 * 
	 * @param buffer
	 *            the FrameBuffer on which to draw to
	 */
	public void draw(FrameBuffer buffer) {
		if (visible) {
			for (GUIComponent c : comps) {
				c.draw(buffer);
			}
		}
	}

}
