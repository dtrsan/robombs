package robombs.game.gui;

import robombs.game.util.*;

import com.threed.jpct.*;
import com.threed.jpct.util.*;
import java.util.*;


	/**
	 * A simple label component for displaying text in the GUI.
	 */
public class Image extends GUIComponent {
    private int xp = 0;
    private int yp = 0;
    private int xSize = 0;
    private int ySize = 0;
    private Texture img=null;
    
    private Map<String, Texture> cacheMap=new HashMap<String, Texture>();

    /**
     * Creates a new label at the given position (top left corner).
     * @param xpos the x-position
     * @param ypos the y-position
     */
    public Image(int xpos, int ypos, int xSize, int ySize, String imgPath) {
        this.xp = xpos;
        this.yp = ypos;
        this.xSize = xSize;
        this.ySize = ySize;
        
        if (imgPath!=null) {
        	setImage(imgPath);
        }
    }

    public boolean evaluateInput(MouseMapper mouse, KeyMapper keyMapper) {
        return super.evaluateInput(mouse, keyMapper);
    }

    public void setImage(String path) {
    	if (cacheMap.get(path)==null) {
	    	 try {
	 	        SimpleStream ss=new SimpleStream(path);
	 	        img=new Texture(ss.getStream());
	 	       cacheMap.put(path, img);
	 	        ss.close();
	         } catch(Exception e) {
	        	 e.printStackTrace();
	        	 System.err.println("Unable to load image: "+path);
	         }
    	} else {
    		img=cacheMap.get(path);
    	}
    }
    
    public void draw(FrameBuffer buffer) {
        if (visible) {
        	if (img!=null) {
        		buffer.blit(img, 0, 0, getParentX()+xp, getParentY()+yp, img.getWidth(), img.getHeight(), xSize, ySize, -1, false, null);
        	}
            super.draw(buffer);
        }
    }
}

	
