package robombs.clientserver;

import java.io.*;
import java.util.*;
import java.util.zip.*;

/**
 * DataContainers are the basic transfer containers for data in this little client/server-world. Each transfer from or to
 * a server or a client will most likely contain at least one DataContainer dropped into an array of DataContainers which itself will be wrapped into an array of bytes.<br>
 * A transfer can contain multiple DataContainers of different types. What's stored into these containers is up to the implementation.
 * <br>One can easily extend the DataContainer to wrap complex data in a convenient way.
 */
public class DataContainer implements Cloneable {

    public final static byte TYPE_INT = 0;
    public final static byte TYPE_FLOAT = 1;
    public final static byte TYPE_STRING = 2;
    public final static byte TYPE_BYTE = 3;

    private final static Byte I_TYPE_INT = Byte.valueOf(TYPE_INT);
    private final static Byte I_TYPE_FLOAT = Byte.valueOf(TYPE_FLOAT);
    private final static Byte I_TYPE_STRING = Byte.valueOf(TYPE_STRING);
    private final static Byte I_TYPE_BYTE = Byte.valueOf(TYPE_BYTE);

    protected List<Object> objs = new ArrayList<Object>(200);
    protected List<Byte> types = new ArrayList<Byte>(200);
    protected int pos = 0;
    protected ClientInfo ci = null;
    protected int msgType = MessageTypes.OBJ_TRANSFER;

    private byte[] bytes = null;
    private int inLen = 0;
    private boolean zip = false;
    private boolean extracted = false;
    
    private static Map<Object, byte[]> cache=Collections.synchronizedMap(new ByteArrayCache<Object, byte[]>());
    private static volatile long hits=0;
    private static volatile long misses=0;
    
    /**
     * Creates a new, empty DataContainer.
     */
    public DataContainer() {
    }

    /**
     * Creates a new DataContainer and populates it with the data wrapped in the given byte arrays.
     * @param bytes the data for this container
     */
    public DataContainer(byte[] bytes) {
        this(bytes, false);
    }
    
    /**
     * Creates a new DataContainer and fills it with the date from the given container.
     * @param dc
     */
    public DataContainer(DataContainer dc) {
    	objs=dc.objs;
    	types=dc.types;
    	pos=dc.pos;
    	ci=dc.ci;
    	msgType=dc.msgType;
    	bytes=dc.bytes;
    	inLen=dc.inLen;
    	zip=dc.zip;
    	extracted=dc.extracted;
    }

    /**
     * Creates a new DataContainer and populates it with the data wrapped in the given byte arrays.
     * @param bytes the data for this container
     * @param zip is the data in this array zipped?
     */
    public DataContainer(byte[] bytes, boolean zip) {
    	inLen = bytes.length;
        if (inLen > 1) {
            this.zip = zip;
            this.bytes = bytes;
            msgType = ((bytes[2] & 0xff) << 8) + (bytes[3] & 0xff);
        } else {
            NetLogger.log("Invalid package size: " + inLen);
        }
    }

    /**
     * Returns the length of the byte array from which this container has been created. If it hasn't, it's 0.
     * @return int the length or 0
     */
    public int getLength() {
        return inLen;
    }

    /**
     * Returns the number of entries in this container
     * @return int the number of entries
     */
    public int getBufferLength() {
        extract();
        return objs.size();
    }

    public int getCurrentSize() {
    	return objs.size();
    }
    
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    /**
     * Returns the raw data (after unzipping) if this DataContainer has been created from a byte array. If it hasn't, this method
     * returns null. This method is useful for extending DataContainer so that the extended instance can be created from an instance of
     * DataContainer simply by feeding the raw data back into super(...);
     * @return byte[] the raw data. Another DataContainer can be created from this one.
     */
    public byte[] getRawData() {
        extract();
        return bytes;
    }

    /**
     * Sets the ClientInfo, the info about the client from which this container came. This information isn't transfered, it's
     * set by the server itself when creating the containers from the stream.
     * @param c the client info
     */
    public void setClientInfo(ClientInfo c) {
        ci = c;
    }

    /**
     * Returns the ClientInfo, the info about the client from which this container came. This information isn't transfered, it's
     * set by the server itself when creating the containers from the stream.
     * @return ClientInfo the client info
     */

    public ClientInfo getClientInfo() {
        return ci;
    }

    /**
     * Gets the message type of this container as defined by MessageTypes.
     * @return int the type
     */
    public int getMessageType() {
        return msgType;
    }

    /**
     * Sets the message type of this container as defined by MessageTypes.
     * @param type the message type
     */
    public void setMessageType(int type) {
        msgType = type;
    }

    /**
     * Adds a new float to the container.
     * @param flt a float
     */
    public void add(float flt) {
        objs.add(Float.valueOf(flt));
        types.add(I_TYPE_FLOAT);
    }

    /**
     * Adds a new int to the container.
     * @param in a int
     */
    public void add(int in) {
        objs.add(Integer.valueOf(in));
        types.add(I_TYPE_INT);
    }

    /**
     * Adds a new byte to the container.
     * @param byt a byte
     */
    public void add(byte byt) {
        objs.add(Byte.valueOf(byt));
        types.add(I_TYPE_BYTE);
    }

    /**
     * Adds a new String to the container.
     * @param str a String
     */
    public void add(String str) {
        objs.add(str);
        types.add(I_TYPE_STRING);
    }

    /**
     * Clears the container. It's empty afterwards.
     */
    public void reset() {
        pos = 0;
    }

    /**
     * Checks, if the container contains more data to read.
     * @return boolean does it?
     */
    public boolean hasData() {
        extract();
        return objs.size() > pos;
    }

    /**
     * Skips an entry in the container.
     * @return DataContainer the same container as return value for convenience reasons.
     */
    public DataContainer skip() {
        extract();
        if (hasData()) {
            pos++;
        } else {
            throw new RuntimeException("No such element!");
        }
        return this;
    }

    /**
     * Returns the type of the next entry in the container.
     * @return byte the type
     */
    public byte getType() {
        extract();
        return ((Byte) types.get(pos)).byteValue();
    }

    /**
     * Reads the next float from the container. If it's not a float, this will cause an ClassCastException.
     * @return float the next float
     */
    public float getNextFloat() {
        extract();
        return ((Float) objs.get(pos++)).floatValue();
    }

    /**
     * Reads the next int from the container. If it's not an int, this will cause an ClassCastException.
     * @return int the next int
     */
    public int getNextInt() {
        extract();
        return ((Integer) objs.get(pos++)).intValue();
    }

    /**
     * Reads the next byte from the container. If it's not a byte, this will cause an ClassCastException.
     * @return byte the next byte
     */
    public byte getNextByte() {
        extract();
        return ((Byte) objs.get(pos++)).byteValue();
    }

    /**
     * Reads the next string from the container. If it's not a string, this will cause an ClassCastException.
     * @return String the next string
     */
    public String getNextString() {
        extract();
        return (String) objs.get(pos++);
    }

    /**
     * Wraps the container's data into a byte array.
     * @param zip boolean
     * @return byte[]
     */
    byte[] toByteArray(boolean zip) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(500);
    	try {
            for (int i = 0; i < objs.size(); i++) {
                // Type schreiben...
                byte type = ((Byte) types.get(i)).byteValue();
                bos.write(new byte[] {type});
                if (type == TYPE_STRING) {
                    // String
                	String txt=(String) objs.get(i);
                	byte[] content=(byte[]) cache.get(txt);
                    int ii = txt.length();
                    int hi = (byte) (ii >> 8);
                    int low = (byte) (ii - (hi << 8));
                    bos.write(new byte[] {(byte) hi, (byte) low}); // Length
                    if (content==null) {
                    	content=((String) objs.get(i)).getBytes("UTF-8");
                    	cache.put(txt, content);
                    	misses++;
                    } else {
                    	hits++;
                    }
                    bos.write(content);
                }
                if (type == TYPE_INT) {
                	Integer in=(Integer) objs.get(i);
                	byte[] content=(byte[]) cache.get(in);
                	if (content==null) {
                		int ii = in.intValue();
                		content=new byte[] {(byte) (ii >> 24), (byte) ((ii >> 16) & 0xFF), (byte) ((ii >> 8) & 0xFF), (byte) (ii & 0xFF)};
                		cache.put(in, content);
                		misses++;
                	} else {
                		hits++;
                	}
                	bos.write(content);
                }
                if (type == TYPE_FLOAT) {
                	Float flo=(Float) objs.get(i);
                	byte[] content=(byte[]) cache.get(flo);
                	if (content==null) {
                		int ii = Float.floatToIntBits(flo.floatValue());
                		content=new byte[] {(byte) (ii >> 24), (byte) ((ii >> 16) & 0xFF), (byte) ((ii >> 8) & 0xFF), (byte) (ii & 0xFF)};
                		cache.put(flo, content);
                		misses++;
                	} else {
                		hits++;
                	}
                	bos.write(content);
                }
                if (type == TYPE_BYTE) {
                    bos.write(((Byte) objs.get(i)).byteValue());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        byte[] bs = bos.toByteArray();
        byte[] bts = new byte[bs.length + 4];
        System.arraycopy(bs, 0, bts, 4, bs.length);

        int ii = msgType;
        bts[2] = (byte) (ii >> 8);
        bts[3] = (byte) (ii & 0xff);

        if (zip) {
            bts = zip(bts);
        }

        ii = bts.length;
        bts[0] = (byte) (ii >> 8);
        bts[1] = (byte) (ii & 0xff);

        /*
        if (misses!=0) {
        	System.out.println(hits*100/(misses+hits)+"/"+cache.size());
        }
        */
        
        return bts;
    }

    /**
     * Sets zip mode for this container
     * @param zip zipped?
     */
    void setZip(boolean zip) {
        this.zip = zip;
    }

    /**
     * Gets the zip mode of this container
     * @return boolean zipped?
     */
    boolean getZip() {
        return zip;
    }

    /**
     * Reads the wrapped data from an byte array and fills the DataContainer with it.
     */
    private void extract() {
        if (!extracted) {
            int len = ((bytes[0] & 0xff) << 8) + (bytes[1] & 0xff);
            if (zip) {
                bytes = unzip(bytes, len);
                len = bytes.length;
                bytes[0] = (byte) (len >> 8);
                bytes[1] = (byte) (len - ((len >> 8) << 8));
            }
            if (bytes.length > 3) {
                try {
                    for (int i = 4; i < len; ) {
                        byte type = bytes[i];
                        switch (type) {
                        case (TYPE_INT): {
                            types.add(I_TYPE_INT);
                            objs.add(Integer.valueOf(((bytes[i + 1] & 0xFF) << 24) + ((bytes[i + 2] & 0xFF) << 16) + ((bytes[i + 3] & 0xFF) << 8) + (bytes[i + 4] & 0xFF)));
                            i += 5;
                            break;
                        }
                        case (TYPE_BYTE): {
                            types.add(I_TYPE_BYTE);
                            objs.add(Byte.valueOf(bytes[i + 1]));
                            i += 2;
                            break;
                        }
                        case (TYPE_FLOAT): {
                            types.add(I_TYPE_FLOAT);
                            float fl = Float.intBitsToFloat(((bytes[i + 1] & 0xFF) << 24) + ((bytes[i + 2] & 0xFF) << 16) + ((bytes[i + 3] & 0xFF) << 8) + (bytes[i + 4] & 0xFF));
                            objs.add(Float.valueOf(fl));
                            i += 5;
                            break;
                        }
                        case (TYPE_STRING): {
                            types.add(I_TYPE_STRING);
                            int leny = (bytes[i + 1] << 8) + bytes[i + 2];
                            objs.add(new String(bytes, i + 3, leny, "UTF-8"));
                            i += 3 + leny;
                            break;
                        }
                        default: {
                            throw new RuntimeException("Unable to detect data type (" + type + ")!");
                        }
                        }
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            pos = 0;
            extracted = true;
        }
    }

    /**
     * Zips the data in the array and returns a zipped version.
     * @param data byte[]
     * @return byte[]
     */
    private byte[] zip(byte[] data) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(data.length);
        bos.write(data, 0, 4);
        try {
            GZIPOutputStream zos = new GZIPOutputStream(bos);
            zos.write(data, 4, data.length - 4);
            zos.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return bos.toByteArray();
    }

    /**
     * Unzip the data in an array
     * @param data byte[]
     * @param len int
     * @return byte[]
     */
    private byte[] unzip(byte[] data, int len) {
        ByteArrayInputStream bis = new ByteArrayInputStream(data, 4, len - 4);
        ByteArrayOutputStream bos = new ByteArrayOutputStream(data.length<<1);
        bos.write(data, 0, 4);
        try {
            GZIPInputStream zis = new GZIPInputStream(bis);
            byte[] buffer = new byte[len];
            int cnt = 0;
            do {
                cnt = zis.read(buffer);
                if (cnt != -1) {
                    bos.write(buffer, 0, cnt);
                }
            } while (cnt >= 0);
            zis.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return bos.toByteArray();
    }
    
    private static class ByteArrayCache<K, V> extends LinkedHashMap<Object, byte[]> {
		
    	protected final static long serialVersionUID=1;
    	private final static int SIZE=200;
    	
    	public ByteArrayCache() {
    		super(SIZE, 0.75f, true);
		}
		
		protected boolean removeEldestEntry(Map.Entry<Object, byte[]> eldest) {
		    return size() > SIZE;
		}
    }
    
}
