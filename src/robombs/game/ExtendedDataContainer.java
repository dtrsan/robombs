package robombs.game;

import robombs.clientserver.*;
import robombs.game.model.*;

import com.threed.jpct.*;

/**
 * An extended data container that offers support for transfering vectors, matrices and such. The container
 * may contains multiple data sets.
 */
public class ExtendedDataContainer extends DataContainer {

	private float[] dump = new float[16];
	private final static float IDENTITY_FLAG=-9999999.12345f;
	
	// If true, all matrices will be transfered as 3x3 instead of 4x4. In the context of this game,
	// this should be sufficient.
	private boolean transferCompressedMatrices=true;
	
    /**
     * Create a new container.
     */
    public ExtendedDataContainer() {
        super();
    }

    /**
     * Build a new extended data container from the content of a generic data container.
     * @param dc DataContainer the generic data container
     */
    public ExtendedDataContainer(DataContainer dc) {
        super(dc);
        setClientInfo(dc.getClientInfo());
        setMessageType(dc.getMessageType());
    }

    public void add(boolean val) {
    	if (val) {
    		add((byte)1);
    	} else {
    		add((byte)0);
    	}
    }
    
    /**
     * Add a matrix to the container.
     * @param mat Matrix the matrix to add
     */
    public void add(Matrix mat) {
        float[] rotDump = mat.getDump();
        if (transferCompressedMatrices) {
        	if (mat.isIdentity()) {
        		add(IDENTITY_FLAG);
        	} else {
		        for (int i = 0; i < rotDump.length-4; i++) {
		        	if ((i+1)%4!=0) {
		        		add(rotDump[i]);
		        	} else {
		        		if (rotDump[i]!=0) {
		        			throw new RuntimeException("This is a 4x4 matrix ("+i+")! Compressed mode isn't possible with such data!");
		        		}
		        	}
		        }
		        for (int i=rotDump.length-4; i<rotDump.length-1; i++) {
		        	if (rotDump[i]!=0) {
	        			throw new RuntimeException("This is a 4x4 matrix ("+i+")! Compressed mode isn't possible with such data!");
	        		}
		        }
        	}
        } else {
        	for (int i = 0; i < rotDump.length; i++) {
	        	add(rotDump[i]);
	        }
        }
    }

    /**
     * Returns the next matrix from the container (if there is one, otherwise an identity matrix will be returned).
     * @return Matrix the matrix
     */
    public Matrix getMatrix() {
        if (hasData()) {
        	if (transferCompressedMatrices) {
        		for (int i = 0; i < 12; i++) {
        			if ((i+1)%4!=0) {
        				dump[i] = getNextFloat();
        				if (dump[i]==IDENTITY_FLAG) {
        					return new Matrix();
        				}
        			} else {
        				dump[i]=0;
        			}
	            }
        		dump[12]=0;
        		dump[13]=0;
        		dump[14]=0;
        		dump[15]=1;
	            Matrix m = new Matrix();
	            m.setDump(dump);
	            return m;
        	} else {
	            for (int i = 0; i < 16; i++) {
	                dump[i] = getNextFloat();
	            }
	            Matrix m = new Matrix();
	            m.setDump(dump);
	            return m;
        	}
        } else {
            return new Matrix();
        }
    }

    /**
     * Adds a new SimpleVector to the container.
     * @param sv SimpleVector the vector
     */
    public void add(SimpleVector sv) {
    	if (sv.x==0 && sv.y==0 && sv.z==0) {
    		add(IDENTITY_FLAG);
    	} else {
	        add(sv.x);
	        add(sv.y);
	        add(sv.z);
    	}
    }

    /**
     * Adds a new LocalObject to the container.
     * @param lo LocalObject the object
     */
    public void add(LocalObject lo) {
        add(lo.getClientID());
        add(lo.getObjectID());
        add((byte)lo.getType());
        add(lo.getAnimation());
        add(lo.getAnimationSpeed());
        add(lo.getRotation());
        add(lo.getPosition());
        add(lo.getSpeed());
        add(lo.getValue());
        add(lo.getSpecialValue());
        add(lo.isInvincible());
    }
    
    public void add(long val) {
    	long v2=(val>>32);
    	int i1=(int) v2;
    	int i2=(int) (val-(v2<<32));
    	add(i1);
    	add(i2);
    }

    /**
     * Fills an existing local object with the data from the container. The id will be ignored.
     * @param lo LocalObject the object to fill
     */
    public void fillLocalObject(LocalObject lo) {
        skip(); // ID nicht!
        lo.setObjectID(getNextInt());
        lo.setType(getNextByte());
        lo.setAnimation(getNextInt());
        lo.setAnimationSpeed(getNextInt());
        lo.setRotation(getMatrix());
        lo.setPosition(getSimpleVector());
        lo.setSpeed(getSimpleVector());
        lo.setValue(getNextInt());
        lo.setSpecialValue(getNextLong());
        lo.setInvincible(getNextBoolean());
    }

    public long getNextLong() {
    	int i1=getNextInt();
    	int i2=getNextInt();
    	long res=((long)i1<<32)+(i2>=0?(long)i2:(4294967294L+(long)i2+2));
    	return res;
    }

    public boolean getNextBoolean() {
    	byte b=getNextByte();
    	return b==1;
    }
    
    /**
     * Creates and returns a new LocalObject based on the data in the container.
     * @return LocalObject the object
     */
    public LocalObject getLocalObject() {
        LocalObject lo = new LocalObject(getNextInt(), true);
        lo.setObjectID(getNextInt());
        lo.setType(getNextByte());
        lo.setAnimation(getNextInt());
        lo.setAnimationSpeed(getNextInt());
        lo.setRotation(getMatrix());
        lo.setPosition(getSimpleVector());
        lo.setSpeed(getSimpleVector());
        lo.setValue(getNextInt());
        lo.setSpecialValue(getNextLong());
        lo.setInvincible(getNextBoolean());
        return lo;
    }

    /**
     * Returns the next SimpleVector from the container (is there is one, otherwise a null-vector will be returned).
     * @return SimpleVector the vector
     */
    public SimpleVector getSimpleVector() {
        if (hasData()) {
        	float f=getNextFloat();
        	if (f==IDENTITY_FLAG) {
        		return new SimpleVector();
        	}
            return new SimpleVector(f, getNextFloat(), getNextFloat());
        } else {
            return new SimpleVector();
        }
    }
}
