package robombs.game.view;

import com.threed.jpct.*;

import java.util.*;
import java.awt.*;

import robombs.game.Globals;
import robombs.game.model.*;

/**
 * A small and dump factory that creates client objects. 
 */
public class ClientObjectFactory {

	private final static Color COLOR=new Color(20,20,20);

    private static ClientObjectFactory instance = null;
    private Map<Integer, Object3D> bluePrints = new HashMap<Integer, Object3D>();

    private ClientObjectFactory() {
        try {
            EnemyView e = new EnemyView();
            rotate(e);
            bluePrints.put(Integer.valueOf(Types.PLAYER), e);

            WeaponView w = new WeaponView();
            rotate(w);
            bluePrints.put(Integer.valueOf(Types.WEAPON), w);

            BombView c = new BombView();
            bluePrints.put(Integer.valueOf(Types.BOMB), c);

            CrateView cr = new CrateView();
            bluePrints.put(Integer.valueOf(Types.CRATE), cr);

            ItemView i1 = new ItemView(Color.WHITE, Types.BOMB_ITEM);
            bluePrints.put(Integer.valueOf(Types.BOMB_ITEM), i1);

            ItemView i2 = new ItemView(Color.WHITE, Types.FIREPOWER_ITEM);
            bluePrints.put(Integer.valueOf(Types.FIREPOWER_ITEM), i2);

            ItemView i3 = new ItemView(Color.WHITE, Types.KICK_ITEM);
            bluePrints.put(Integer.valueOf(Types.KICK_ITEM), i3);

			ItemView i4 = new ItemView(Color.WHITE, Types.DISEASE_ITEM);
            bluePrints.put(Integer.valueOf(Types.DISEASE_ITEM), i4);

            if (Globals.compiledObjects) {
            	e.compile(true);
            	w.compile(true);
            	c.compile(Globals.allDynamic);
            	c.getChild().compile(Globals.allDynamic);
            	cr.compile(Globals.allDynamic);
            	i1.compile(Globals.allDynamic);
            	i2.compile(Globals.allDynamic);
            	i3.compile(Globals.allDynamic);
            	i4.compile(Globals.allDynamic);
            }
            
        } catch (Exception e) {
            // Should not happen...
            throw new RuntimeException(e);
        }
    }

    /**
     * Get the instance. The Factory is a singleton.
     * @return ClientObjectFactory the one and only instance
     */
    public static synchronized ClientObjectFactory getInstance() {
        if (instance == null) {
            instance = new ClientObjectFactory();
        }
        return instance;
    }

    /**
     * Creates a new vlient object of the given type.
     * @param type the type
     * @return ClientObject the new object
     */
    public ClientObject create(int type) {
        try {
            ClientObject obj = null;
            switch (type) {
            case (Types.PLAYER):
                obj = new EnemyView(bluePrints.get(Integer.valueOf(type)), bluePrints.get(Integer.valueOf(Types.WEAPON)));
                obj.setCollisionMode(Object3D.COLLISION_CHECK_OTHERS);
                obj.setCollisionOptimization(Object3D.COLLISION_DETECTION_OPTIMIZED);
                obj.setAdditionalColor(COLOR);
                break;
            case (Types.LOCAL_PLAYER):
                obj = new EnemyView(bluePrints.get(Integer.valueOf(Types.PLAYER)), bluePrints.get(Integer.valueOf(Types.WEAPON)));
           		obj.setAdditionalColor(COLOR);
           		break;
            case (Types.BOMB):
                obj = new BombView((ClientObject) bluePrints.get(Integer.valueOf(type)));
                break;
            case (Types.CRATE):
                obj = new CrateView((ClientObject) bluePrints.get(Integer.valueOf(type)));
                break;
            case (Types.BOMB_ITEM):
                obj = new ItemView((ClientObject) bluePrints.get(new Integer(type)), Color.BLUE, type);
                break;
            case (Types.FIREPOWER_ITEM):
                obj = new ItemView((ClientObject) bluePrints.get(new Integer(type)), Color.ORANGE, type);
                break;
            case (Types.KICK_ITEM):
                obj = new ItemView((ClientObject) bluePrints.get(new Integer(type)), Color.RED, type);
                break;
             case (Types.DISEASE_ITEM):
			    obj = new ItemView((ClientObject) bluePrints.get(new Integer(type)), Color.BLACK, type);
                break;
            default:
                throw new RuntimeException("Unknown type: " + type);
            }
            return obj;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void rotate(Object3D obj) {
        obj.rotateY( -(float) Math.PI / 2f);
        obj.rotateMesh();
        obj.getRotationMatrix().setIdentity();
        obj.build();
    }
}
