package robombs.game.view;

import java.util.*;
import com.threed.jpct.*;
import robombs.game.*;

public class ExplosionManager {

	private List<Explosion> explosions=null;
	private World world=null;
	
	public ExplosionManager(World world) {
		explosions=new ArrayList<Explosion>();
		this.world=world;
	}
	
	public void addExplosion(ClientObject bomb, ClientEventQueue eventQueue, Level level) {
		Explosion e=null;
		for (Explosion ex: explosions) {
			if (!ex.getVisibility()) {
				e=ex;
				e.setLocal(!bomb.isRemote());
				break;
			}
		}
		if (e==null) {
			e=new Explosion(!bomb.isRemote());
			e.setEventQueue(eventQueue);
			explosions.add(e);
			e.addToWorld(world);
		}
		e.enable(bomb.getTranslation(), bomb);
		if (level!=null) {
			SimpleVector pos=new SimpleVector(bomb.getTranslation());
			//pos.add(new SimpleVector(0,-2f,0));
			level.getSmokeCloudManager().addSmokeEmitter(pos);
		}
	}
	
	public void process(long ticks) {
		for (Explosion ex: explosions) {
			if (ex.getVisibility()) {
				ex.process(ticks);
			}
		}
	}
}
