package robombs.game.view;

import robombs.game.Event;
import robombs.game.model.*;

import com.threed.jpct.*;

import java.awt.*;

public class ItemView extends ClientObject {

	static final private long serialVersionUID = 1L;

	private static ClientObject bluePrint = null;
	private transient LocalObject lob=null;
	private int type=0;
	private float scale=0.2f;
	private float trans=0;
	private boolean collected=false;
	
	static {
		Object3D obj=Primitives.getDoubleCone(6, 4);
		obj.setTexture("item");
		obj.setEnvmapped(Object3D.ENVMAP_ENABLED);
		obj.setEnvmapMode(Object3D.ENVMAP_CAMERASPACE);
		obj.setEnvmapDirection(Object3D.ENVMAP_YZ);
		obj.setTransparency(15);
		obj.getMesh().compress();
		obj.build();
		bluePrint=new ClientObject(obj);
		bluePrint.translate(10000, 10000, 10000);
	}

	/**
	 * Creates a new item.
	 */
	public ItemView(Color col, int type) {
		super(bluePrint, false);
		this.setRenderHook(null);
		this.setAdditionalColor(col);
		this.type=type;
		createChild(type);
	}

	public ItemView(ClientObject obj, Color col, int type) {
		super(obj, true);
		this.setRenderHook(null);
		this.setAdditionalColor(col);
		this.type=type;
		createChild(type);
	}
	
	private void createChild(int type) {
		addChild(Primitives.getPlane(1, 3));
		Object3D child=getChild();
		
		Matrix m=child.getRotationMatrix();
		float[] d=m.getDump();
		d[0]=1.4f;
		m.setDump(d);
		
		child.setCulling(Object3D.CULLING_DISABLED);
		child.translate(0, -6.5f, 0);
		child.setAdditionalColor(Color.WHITE);
		child.rotateZ(-(float)Math.PI/2f);
		
		PolygonManager pm=child.getPolygonManager();
		int tid=TextureManager.getInstance().getTextureID("icons");
		
		float w=128f;
		float s=0;
		float e=0;
	
		//private static int[] starts=new int[]{0,23,45,67,91,112};

		switch(type) {
			case Types.BOMB_ITEM: s=24;e=45;break;
			case Types.FIREPOWER_ITEM: s=0;e=23;break; 
			case Types.KICK_ITEM: s=69f;e=92;break; 
			case Types.DISEASE_ITEM: s=93;e=115;break; 
		}
		
		TextureInfo ti=new TextureInfo(tid, s/w,0,e/w,0,s/w,1);
		pm.setPolygonTexture(0, ti);
		ti=new TextureInfo(tid, e/w,0,e/w,1,s/w,1);
		pm.setPolygonTexture(1, ti);
		child.setTransparency(17);
		child.build();
	}

	public void hitByLocalPlayer(CollisionParticipant source, LocalObject obj, DecalManager decal, CollisionEvent ce) {

		if (!collected) {
			// Before sending the event, we already disable collision checks on this one, because otherwise
			// we may get some noticable hickups in movement.
			setCollisionMode(Object3D.COLLISION_CHECK_NONE);
			collected=true;

			Event event=null;
			if (type==Types.BOMB_ITEM) {
				event = new Event(Event.BOMB_ITEM_COLLECTED, source.getObjectID(), obj.getObjectID(), -99);
				event.setSourceClientID(source.getClientID());
				event.setOrigin(getTranslation());
			}
			if (type==Types.FIREPOWER_ITEM) {
				event = new Event(Event.FIREPOWER_ITEM_COLLECTED, source.getObjectID(), obj.getObjectID(), -99);
				event.setSourceClientID(source.getClientID());
				event.setOrigin(getTranslation());
			}
			if (type==Types.KICK_ITEM) {
				event = new Event(Event.KICK_ITEM_COLLECTED, source.getObjectID(), obj.getObjectID(), -99);
				event.setSourceClientID(source.getClientID());
				event.setOrigin(getTranslation());
			}
			if (type==Types.DISEASE_ITEM) {
				event = new Event(Event.DISEASE_ITEM_COLLECTED, source.getObjectID(), obj.getObjectID(), -99);
				event.setSourceClientID(source.getClientID());
				event.setOrigin(getTranslation());
			}
			source.getEventQueue().add(event);
		}
	}


	public void init(int id) {
		// This instance of LocalObject is used in the view only. The items aren't real
		// LocalObject/ClientObject-entities as they are not transfered to the server.
		// However, events and collision listeners need some information from the LocalObject
		lob=new LocalObject(-99);
		lob.setObjectID(id);
	}

	public LocalObject getLocalObject() {
		return lob;
	}

	public void process(long ticks, Level level) {
		setScale(scale);
		setTransparency((int) trans);
		if (scale<1) {
			scale+=(float) ticks*0.05f;
			if (scale>1) {
				scale=1;
			}
			trans=15f*scale;
		} else {
			if (!collected) {
				setCollisionMode(Object3D.COLLISION_CHECK_OTHERS);
			}
		}
		rotateY(0.01f*(float) ticks);
		getChild().rotateY(-0.07f*(float) ticks);
    }



}
