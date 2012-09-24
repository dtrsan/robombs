package robombs.game.view;

import java.util.*;
import com.threed.jpct.*;
import com.threed.jpct.util.*;

import robombs.clientserver.*;
import robombs.game.model.*;
import robombs.game.*;

public class ItemManager {

	private List<ItemView> items=null;
	private World world=null;
	private ShadowHelper sh=null;

	public ItemManager(World world) {
		items=new ArrayList<ItemView>();
		this.world=world;
	}

	public void addItem(SimpleVector position, int id, int type, ShadowHelper sh, ClientEventQueue eventQueue) {
		ItemView e=(ItemView) ClientObjectFactory.getInstance().create(type);
		e.setEventQueue(eventQueue);
		items.add(e);
		e.addToWorld(world);
		if (sh!=null) {
			sh.addCaster(e);
			this.sh=sh;
		}
		e.init(id);
		e.addCollisionListener(new ClientObjectCollisionListener(e.getLocalObject(), null, true));
		e.getTranslationMatrix().setIdentity();
		e.translate(position);
	}

	public void makeCaster(ShadowHelper sh, ItemView item) {
		if (sh!=null) {
			sh.addCaster(item);
		}
	}

	public void process(long ticks, Level level) {
		for (ItemView ex: items) {
			if (ex.getVisibility()) {
				ex.process(ticks, level);
			}
		}
	}

	public boolean collect(Event event, Level level, boolean isLocal, LocalPlayerObject lob) {
		int id=event.getTargetID();
		ItemView item=null;
		for (ItemView cv:items) {
			if (cv.getLocalObject().getObjectID()==id && cv.getVisibility()) {
				cv.setVisibility(false);
				if (sh!=null) {
					sh.removeCaster(cv);
				}
				cv.removeFromWorld(world);
				item=cv;
				break;
			}
		}

		boolean ret=true;
		
		if (isLocal && item!=null) {
			// The player of this client has collected the item, so transfer its powers to him!
			SimpleVector c=item.getTransformedCenter();
			GridPosition pos=level.getMask().getGrid(c.x, c.z);
			int itemType=level.getMask().getMaskAt(pos);
			if (itemType==MapMask.BOMB_ITEM) {
				lob.getPlayerPowers().addToBombCount(1);
				NetLogger.log("Player has increased the maximum number of bombs!");
			}
			if (itemType==MapMask.FIREPOWER_ITEM) {
				lob.getPlayerPowers().addToFirePower(10);
				NetLogger.log("Player has increased the power of the bombs!");
			}
			if (itemType==MapMask.KICK_ITEM) {
				lob.getPlayerPowers().setKick(true);
				NetLogger.log("Player can now kick bombs!");
			}
			if (itemType==MapMask.DISEASE_ITEM) {
				lob.getPlayerPowers().makeSick();
				NetLogger.log("Player "+lob.getPlayerPowers().getSicknessDescription());
				ret=false;
			}
		} else {
			if (item!=null) {
				// Not local? Just determine the item type (bonus/malus)
				SimpleVector c=item.getTransformedCenter();
				GridPosition pos=level.getMask().getGrid(c.x, c.z);
				int itemType=level.getMask().getMaskAt(pos);
				ret=(itemType!=MapMask.DISEASE_ITEM);
			}
		}
		
		if(item!=null) {
			clearGrid(level, item);
		} else {
			NetLogger.log("Missing item!?");
		}
		
		return ret;
	}

	public void clearGrid(Level level, SimpleVector pos) {
		GridPosition gp=level.getMask().getGrid(pos.x, pos.z);
		level.getMask().setMaskAt(gp, MapMask.FLOOR);
	}

	private void clearGrid(Level level, ItemView crate) {
		clearGrid(level, crate.getTransformedCenter());
	}
}
