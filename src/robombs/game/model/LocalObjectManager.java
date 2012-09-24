package robombs.game.model;

import java.util.*;

import com.threed.jpct.SimpleVector;

import robombs.game.*;
import robombs.game.view.ClientObject;

public abstract class LocalObjectManager {

	protected List<LocalObject> objs=new ArrayList<LocalObject>();

    /**
     * The collision detection routines work with the view objects, not with the model. Therefore,
     * it's required to map the view to the model. To minimize dependencies, this is done by using this
     * method.
     * @param co the view
     * @return LocalObject the corresponding model (or null if there is none...what shouldn't happen)
     */
    public LocalObject getLocalObjectToView(ClientObject co) {
        for (Iterator<LocalObject> itty=objs.iterator(); itty.hasNext();) {
        	LocalObject lob=itty.next();
            if (co.equals(lob.getView())) {
                return lob;
            }
        }
        return null;
    }
    
    public List<SimpleVector> getObjectPositions() {
		return getObjectPositions(-999999);
	}
    
    public List<SimpleVector> getObjectPositions(int type) {
		List<SimpleVector> res=new ArrayList<SimpleVector>();
		for (LocalObject lob: objs) {
			if (lob.getView()!=null && lob.getView().getVisibility() && (type==-999999 || lob.getType()==Types.PLAYER)) {
				res.add(lob.getPosition());
			}
		}
		return res;
	}
    
    /**
     * Prepares a data container with the data of all currently active bullets on a client and adds
     * all still exisiting but inactive bullets to a "to-kill"-List.
     * @param dc the container to be filled
     * @return List the list with inactive bullets that weren't added to the container
     */
    public abstract List<LocalObject> fillOrKill(ExtendedDataContainer dc, ClientEventQueue eventQueue);
      
}
