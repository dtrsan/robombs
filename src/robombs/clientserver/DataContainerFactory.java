package robombs.clientserver;

import java.util.*;

/**
 * A helper class to wrap an array of bytes into one of DataContainers and vice versa. See DataContainer itself
 * for more info on how DataContainers are working and being transfered.
 */
public class DataContainerFactory {

    /**
     * Creates an array for DataContainers from a byte array. Usually, the byte array is what has been transfered
     * from client to server or vice versa.
     * @param bytes the byte array
     * @param zip if true, the data in the array is assumed to be zipped
     * @return DataContainer[] the resulting DataContainers
     */
    public static DataContainer[] extractContainers(byte[] bytes, boolean zip) {
        if (bytes.length > 0) {
            List<DataContainer> conts = new ArrayList<DataContainer>();
            try {
                for (int i = 0; i < bytes.length; ) {
                    int len = ((bytes[i] & 0xff) << 8) + (bytes[i + 1] & 0xff);
                    byte[] snip = new byte[len];
                    System.arraycopy(bytes, i, snip, 0, len);
                    i += len;
                    DataContainer dc = new DataContainer(snip, zip);
                    conts.add(dc);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            DataContainer[] dcs = new DataContainer[conts.size()];
            for (int i = 0; i < conts.size(); i++) {
                dcs[i] = (DataContainer) conts.get(i);
            }
            return dcs;
        } else {
            NetLogger.log("Invalid datagram size: " + bytes.length);
            return new DataContainer[0];
        }
    }

    /**
     * Create a byte array out of an array of DataContainers.
     * @param dcs the containers
     * @param zip boolean should the byte array be zipped?
     * @return byte[] the resulting byte array.
     */
    public static byte[] toByteArray(DataContainer[] dcs, boolean zip) {
        int size = 0;
        byte[][] cs = new byte[dcs.length][];
        for (int i = 0; i < dcs.length; i++) {
            byte[] ba = dcs[i].toByteArray(zip);
            cs[i] = ba;
            size += ba.length;
        }
        size += 2;

        //System.out.println(size);
        
        if (size > 32767) {
            throw new RuntimeException("Datagram size must not exceed 32K but is "+size+" bytes!");
        }

        byte[] res = new byte[size];
        int off = 2;
        for (int i = 0; i < dcs.length; i++) {
            System.arraycopy(cs[i], 0, res, off, cs[i].length);
            off += cs[i].length;
        }
        res[0] = (byte) (size >> 8);
        res[1] = (byte) (size & 0xff);
        return res;
    }
}
