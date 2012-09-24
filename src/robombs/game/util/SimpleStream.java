package robombs.game.util;

import java.io.*;

/**
 * A simple wrapper for InputStreams from the class path.
 */
public class SimpleStream {
    private InputStream stream;

    /**
     * Create a new stream based on a file name. The file has to be present in the class path.
     * @param file the file name.
     */
    public SimpleStream(String file) {
        stream = this.getClass().getClassLoader().getResourceAsStream(file);
        
        if (stream==null) {
        	File fi=new File(file);
        	try {
        		stream=new FileInputStream(fi);
        	} catch(Exception e) {
        		throw new RuntimeException(e);
        	}
        }
        
    }

    /**
     * Returns the actual stream.
     * @return InputStream the stream
     */
    public InputStream getStream() {
        return stream;
    }

    /**
     * Closes the stream.
     * @throws IOException
     */
    public void close() throws IOException {
        if (stream != null) {
            stream.close();
        }
    }
}
