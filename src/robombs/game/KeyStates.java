package robombs.game;

import com.threed.jpct.util.*;
import java.awt.event.*;

/**
 * A very simple class that contains key states. This has to be improved to support key mappings and such things.
 */
public class KeyStates {

    public static boolean up = false;
    public static boolean down = false;
    public static boolean left = false;
    public static boolean right = false;
    public static boolean exit = false;
    public static boolean lookUp = false;
    public static boolean lookDown = false;
    public static boolean fire=false;
    public static boolean bloom = false;
    public static boolean one=false;
    public static boolean two=false;
    public static boolean three=false;
    public static boolean four=false;
    public static boolean five=false;
    public static boolean six=false;
    public static boolean seven=false;
    public static boolean eight=false;
    public static boolean nine=false;
    public static boolean ten=false;
    public static boolean tab=false;
    public static boolean lifeTime=false;
    public static boolean populate=false;
    public static boolean nextLevel=false;
    public static boolean throttle=true;
    public static boolean wireFrame=false;
    public static boolean f1=false;
    public static boolean f2=false;
    public static boolean f3=false;
    
    /**
     * Polls the keys.
     * @param keyMapper KeyMapper the current key mapper
     */
    public static void poll(KeyMapper keyMapper) {
        KeyState ks = null;
        while ((ks = keyMapper.poll()) != KeyState.NONE) {
        	switch(ks.getKeyCode()) {
        		case(KeyEvent.VK_UP):
        		case(KeyEvent.VK_W):
        		case(KeyEvent.VK_NUMPAD8):
        		case(KeyEvent.VK_NUMPAD5):
	                up = ks.getState();
        			break;
        		case(KeyEvent.VK_DOWN):
        		case(KeyEvent.VK_S):
        		case(KeyEvent.VK_NUMPAD2):
	                down = ks.getState();
        			break;
        		case(KeyEvent.VK_SPACE):
	                fire = ks.getState();
        			break;
        		case(KeyEvent.VK_TAB):
	                tab=ks.getState();
        			break;
        		case(KeyEvent.VK_1):
	                one = ks.getState();
        			break;
        		case(KeyEvent.VK_2):
	                two = ks.getState();
        			break;
        		case(KeyEvent.VK_3):
	                three = ks.getState();
        			break;
        		case(KeyEvent.VK_4):
	                four = ks.getState();
        			break;
        		case(KeyEvent.VK_5):
	                five = ks.getState();
        			break;
        		case(KeyEvent.VK_6):
	                six = ks.getState();
        			break;
        		case(KeyEvent.VK_7):
	                seven = ks.getState();
        			break;
        		case(KeyEvent.VK_8):
	                eight = ks.getState();
        			break;
        		case(KeyEvent.VK_9):
	                nine = ks.getState();
        			break;
        		case(KeyEvent.VK_0):
	                ten = ks.getState();
        			break;
        		case(KeyEvent.VK_F1):
        			f1=ks.getState();
        			break;
        		case(KeyEvent.VK_F2):
        			f2=ks.getState();
        			break;
        		case(KeyEvent.VK_F3):
        			f3=ks.getState();
        			break;
        		case(KeyEvent.VK_PAGE_DOWN):
	                lookUp = ks.getState();
        			break;
        		case(KeyEvent.VK_PAGE_UP):
	                lookDown = ks.getState();
        			break;
        		case(KeyEvent.VK_LEFT):
        		case(KeyEvent.VK_A):
        		case(KeyEvent.VK_NUMPAD1):
	                left = ks.getState();
        			break;
        		case(KeyEvent.VK_RIGHT):
        		case(KeyEvent.VK_D):
        		case(KeyEvent.VK_NUMPAD3):
	                right = ks.getState();
        			break;
        		case(KeyEvent.VK_B):
	                if (ks.getState()) {
	                    bloom = !bloom;
	                }
        			break;
        		case(KeyEvent.VK_O):
	                if (ks.getState()) {
	                    wireFrame = !wireFrame;
	                }
        			break;
        		case(KeyEvent.VK_N):
	                if (ks.getState()) {
	                    nextLevel=true;
	                }
	            	break;
	            /*
        		case(KeyEvent.VK_P):
	                if (ks.getState()) {
	                    populate = true;
	                }
        			break;
        		*/
            	/*
        		case(KeyEvent.VK_L):
	                if (ks.getState()) {
	                    lifeTime = !lifeTime;
	                }
	            }
	            */
        		case(KeyEvent.VK_T):
	                if (ks.getState()) {
	                    throttle = !throttle;
	                }
        			break;
        		case(KeyEvent.VK_M):
	            	if (ks.getState()) {
	            		System.runFinalization();
	                    System.gc();
	                    Runtime rt=Runtime.getRuntime();
	                    System.out.println("-=-=-= Memory used: "+(rt.totalMemory()-rt.freeMemory())+" -=-=-=");
	                    System.out.println("-=-=-= Texture memory used: "+(com.threed.jpct.TextureManager.getInstance().getMemoryUsage(false))+" -=-=-=");
	            	}
        			break;

        		case(KeyEvent.VK_ESCAPE):
	                exit = ks.getState();
        			break;
        	}
        }
    }
}
