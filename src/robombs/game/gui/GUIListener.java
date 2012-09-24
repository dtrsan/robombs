package robombs.game.gui;

/**
 * An interface for GUI listeners. An element changed event will be fired if a button has been clicked and similar events.
 */
public interface GUIListener {
        /**
         * Notify the implemention about a change in the GUI.
         * @param label the label of the element in question
         * @param data the data that this element holds (if any, null otherwise)
         */
        void elementChanged(String label, String data);
}
