package robombs.game.view;

import robombs.game.Globals;
import robombs.game.model.*;

import com.threed.jpct.*;


/**
 * An animated object is an extended client object which can use the animation information already present
 * in the client object to play a keyframe animation!
 */
public abstract class AnimatedObject extends ClientObject {

	private static final long serialVersionUID = 1L;

	public AnimatedObject() {
        super(Object3D.createDummyObj());
        if (Globals.compiledObjects) {
        	compile(true);
        }
    }

    public AnimatedObject(Object3D obj) {
        super(obj);
        if (Globals.compiledObjects) {
        	compile(true);
        }
    }

    public AnimatedObject(Object3D obj, Object3D child) {
        super(obj, child);
        if (Globals.compiledObjects) {
        	compile(true);
        	getChild().compile(true);
        }
    }

    /**
     * Updates the view, i.e. animates the object and does the movement interpolation based on linear interpolation
     * until a new state from the server arrives.
     * @param ticks the ticks passed since the last call
     * @param level the level. Not used in within this method ATM.
     */
    public void process(long ticks, Level level) {
        int animSeq = getBackAnimation();
        int animSpeed = getBackAnimationSpeed();

        float offset = 0;
        boolean stop = false;
        if (animSeq > Animations.NOLOOP) {
            animSeq -= Animations.NOLOOP;
            stop = true;
        }

        if (isModified() || animSeq == Animations.DIE) {
        	// Has moved or is dead anyway: Take the position that the server has transmitted
            SimpleVector pos = getBackPosition();
            getTranslationMatrix().setIdentity();
            translate(pos);
            setModified(false);
        } else {
        	// Otherwise, the client wins and the translation will be applied
        	SimpleVector spd = new SimpleVector(getBackSpeed());
            spd.scalarMul(ticks);
            translate(spd);
        }

        Matrix tar = getRotationMatrix().cloneMatrix();
        tar.interpolate(getRotationMatrix(), getBackRotationMatrix(), 0.4f * (float) ticks);
        setRotationMatrix(tar);

        int mul = 1;
        if (!getBackSpeed().equals(SimpleVector.ORIGIN) && getBackSpeed().calcDot(getBackRotationMatrix().getZAxis()) < 0) {
            mul = -1;
        }

        if (getLastSequence() != animSeq) {
            resetTicks();
        }

        setLastSequence(animSeq);
        setAnimSpeed(animSpeed);

        if (animSeq == Animations.DIE) {
            offset = 0.631f; // The death animation that we want to use starts here (why not at 0? Only md2 knows...):
            setClampingMode(Animation.USE_CLAMPING);
        } else {
        	setClampingMode(Animation.USE_WRAPPING);
        }

        long tickCnt = getTicks();
        if (animSpeed != 0) {
        	if (getAnimationSequence()!=null) {
        		// Do this onyl for "visual" clients. Bots don't play this animation.
	            if (!stop) {
	                animate((float) (tickCnt % animSpeed) / (float) animSpeed + offset, animSeq);
	            } else {
	                animate(1f, animSeq);
	            }
        	}
        }
        addToTicks(ticks * mul);
    }
}
