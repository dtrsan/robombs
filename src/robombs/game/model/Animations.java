package robombs.game.model;

/**
 * An interface that defines some constants for animation sequences of the used MD2 files. The used value
 * is transfered to the server to indicate other clients which animation should be played for a particular client.
 */
public interface Animations {
    public final static int NONE=1;
    public final static int MOVE=2;
    public final static int FIRE=3;
    public final static int HIT=5;
    public final static int CROUCH_NONE=11;
    public final static int CROUCH_MOVE=12;
    public final static int DIE=18;

    public final static int NOLOOP=1000000;
    public final static int DEAD=18+NOLOOP;
}
