package robombs.clientserver;

import java.io.*;

/**
 * Helper class to read bytes from an InputStream without reading the whole stream and without blocking if the stream has
 * ended before all data could be read. The first two bytes in each part that should be read define the size in hi-byte/low-byte
 * format. The converter will try to read the number of bytes indicated by this value from the stream and returns with the result.
 */
public class StreamConverter {

        /**
         * Reads data from an InputStream
         * @param is The stream to read from
         * @return byte[] the data from the stream. This is the real payload, i.e. it doesn't include the size information
         */
        public static byte[] convert(InputStream is) {
        byte[] buffer = new byte[500];
        int cnt = 0;
        ByteArrayOutputStream bos = null;
        int pos = 0;
        int len = -1;
        boolean lenOK = false;
        try {
            int off = 0;
            do {
                cnt = is.read(buffer);
                if (cnt > -1) {
                    if (cnt > 0) {
                        off = 0;
                        if (!lenOK || pos < 2) {
                            // Die tatsächliche Länge ermitteln...
                            if (cnt > 1 && pos == 0) {
                                len = ((buffer[0] & 0xff) << 8) + (buffer[1] & 0xff);
                                lenOK = true;
                                off = 2;
                            } else {
                                off = 1;
                                if (pos == 0) {
                                    len = (buffer[0] & 0xff) << 8;
                                } else {
                                    len += buffer[0] & 0xff;
                                    lenOK = true;
                                }
                            }
                        }
                    }
                    
                    if (lenOK && bos==null) {
                    	bos=new ByteArrayOutputStream(len+10);
                    }
                    
                    if (pos + cnt <= len || !lenOK) {
                        bos.write(buffer, off, cnt - off);
                        pos += cnt;
                    } else {
                        bos.write(buffer, off, len - pos - off);
                        pos += len - pos;
                    }
                }
            } while (cnt > -1 && pos < len);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (bos==null) {
        	return new byte[0];
        }
        return bos.toByteArray();
    }
}
