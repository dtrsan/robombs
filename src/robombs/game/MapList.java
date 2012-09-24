package robombs.game;

import java.util.*;
import robombs.game.util.*;
import com.threed.jpct.*;

public class MapList {

	private List<MapInfo> infos=new ArrayList<MapInfo>();
	private int checkSum=999999999;
	
	@SuppressWarnings("unchecked")
	public MapList() {
		Properties props=new Properties();
		try {
			SimpleStream ss= new SimpleStream("data/levels/levels.txt");
			props.load(ss.getStream());
			ss.close();
		} catch(Exception e) {
			throw new RuntimeException("Unable to load levels.txt!", e);
		}
		
		for (Iterator itty=props.keySet().iterator(); itty.hasNext();) {
			String key=itty.next().toString();
			String val=props.getProperty(key);
			MapInfo mi=new MapInfo(key, val);
			infos.add(mi);
		}
		Collections.sort(infos);
	}
	
	public List<MapInfo> getMapInfos() {
		return infos;
	}
	
	public int getCheckSum() {
		if (checkSum==999999999) {
			long i=0;
			for (MapInfo mi:infos) {
				i+=mi.getName().hashCode()*mi.getPicture().hashCode()+mi.getRealName().hashCode()-mi.getRealName().length();
				try {
					SimpleStream ss= new SimpleStream("data/levels/"+mi.getName());
					String level=Loader.loadTextFile(ss.getStream());
					ss.close();
					i+=3*level.hashCode();
				} catch(Exception e) {
					throw new RuntimeException("Unable to load "+mi.getName(), e);
				}
			}
			checkSum=(int) ((i+Float.parseFloat(Globals.GAME_VERSION))%100000000);
		}
		return checkSum;
	}
	
}
