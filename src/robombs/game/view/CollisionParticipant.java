package robombs.game.view;

import robombs.game.model.*;
import com.threed.jpct.*;
import robombs.game.*;

public interface CollisionParticipant {

	void hits(CollisionParticipant target, LocalObject obj, DecalManager decal, CollisionEvent ce);
	
	void hitByExplosion(CollisionParticipant source, LocalObject obj, DecalManager decal, CollisionEvent ce);
	
	void hitByLocalPlayer(CollisionParticipant source, LocalObject obj, DecalManager decal, CollisionEvent ce);

	void setClientID(int id);
	
	void setObjectID(int id);
	
	int getObjectID();
	
	int getClientID();
	
	void setEventQueue(ClientEventQueue q);
	
	ClientEventQueue getEventQueue();
	
}
