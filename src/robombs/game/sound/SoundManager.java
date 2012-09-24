package robombs.game.sound;

import paulscode.sound.*;

import com.threed.jpct.*;
import java.util.*;
import robombs.game.*;

public class SoundManager {

	public final static float ANGLE_NOT_CHANGED=-99999;
	private final static float POS_MUL=-1f;
	
	private static SoundManager instance=null;
	
	private final static long MIN_TICKS=5;
	
	private SoundSystem soundSys=null;
	private Map<String, String> sounds=null;
	private long curTicks=0;
	private float lastAngle=ANGLE_NOT_CHANGED;
	private Map<String, String> mapping=new HashMap<String, String>();
	private SimpleVector listener=new SimpleVector();
	
	
	public static synchronized SoundManager getInstance() {
		if (instance==null) {
			instance=new SoundManager();
		}
		return instance;
	}
	
	private SoundManager() {
		SoundSystemConfig.setSoundFilesPackage("");
		/*
		String osName=System.getProperty("os.name");
		if (FORCE_JAVA_SOUND || (osName!=null && osName.toLowerCase().indexOf("linux")!=-1)) {
			// We have Linux...don't use OpenAL ATM 
			SoundSystemConfig.setDefaultLibrary(SoundSystemConfig.LIBRARY_JAVASOUND);
			Logger.log("Using JavaSound sound system!", Logger.MESSAGE);
		} else {
			Logger.log("Using OpenAL sound system!", Logger.MESSAGE);
		}
		*/
		try {
			SoundSystemConfig.addLibrary(paulscode.sound.libraries.LibraryLWJGLOpenAL.class);
			SoundSystemConfig.addLibrary(paulscode.sound.libraries.LibraryJavaSound.class);
			SoundSystemConfig.setCodec("wav", paulscode.sound.codecs.CodecWav.class);
		} catch (SoundSystemException e) {
			Logger.log(e);
		}
		soundSys=new SoundSystem();
		sounds=new HashMap<String, String>();
	}
	
	public void dispose() {
		if (soundSys!=null) {
			try {
				// wait some time for sounds to finish, unless there is a better method to do this...
				Thread.sleep(300);
			} catch(Exception e) {
				e.printStackTrace();
			}
			soundSys.cleanup();
			instance=null;
		}
	}
	
	public void finalize() {
		dispose();
	}
	
	public void addSound(String name, String fileName) {
		sounds.put(name, fileName);
	}
	
	public void play(String sound, SimpleVector pos) {
		play(sound, pos, false);
	}
	
	public void stop(String sound) {
		if (soundSys!=null) {
			String name=mapping.get(sound);
			if (name!=null) {
				soundSys.stop(name);
				mapping.remove(sound);
			}
		}
	}
	
	public void play(String sound, SimpleVector pos, boolean loop) {
		if (soundSys!=null) {
			if (Globals.mute) {
				return;
			}
			String fn=sounds.get(sound);
			if (fn!=null) {
				try {
					String name=soundSys.quickPlay(false, fn, loop, pos.x, pos.y*POS_MUL, pos.z*POS_MUL, SoundSystemConfig.ATTENUATION_LINEAR,150);
					if (loop) {
						mapping.put(sound, name);
					}
				} catch(Exception e) {
					throw new RuntimeException("Unable to play sound: "+sound+"/"+fn,e);
				}
			} else {
				throw new RuntimeException("Sound '"+sound+"' is unknown to the SoundManager!");
			}
		}
	}
	
	public void move(String sound, SimpleVector pos) {
		if (soundSys!=null) {
			String name=mapping.get(sound);
			if (name!=null) {
				soundSys.setPosition(name, pos.x, pos.x*POS_MUL, pos.x*POS_MUL);
			}
		}
	}
	
	public SimpleVector getListenerPosition() {
		return listener;
	}
	
	public void setListener(SimpleVector pos, float angle) {
		setListener(pos, angle, 99999);
	}
	
	public void setListener(SimpleVector pos, float angle, long ticks) {
		if (soundSys!=null) {
			curTicks+=ticks;
			if (angle!=ANGLE_NOT_CHANGED) {
				lastAngle=angle;
			}
			listener.set(pos);
			if (curTicks>=MIN_TICKS) {
				soundSys.setListenerPosition(pos.x, pos.y*POS_MUL, pos.z*POS_MUL);
				if (lastAngle!=ANGLE_NOT_CHANGED) {
					soundSys.setListenerAngle(lastAngle);
					lastAngle=ANGLE_NOT_CHANGED;
				}
				curTicks=0;
			}
		}
	}
}
