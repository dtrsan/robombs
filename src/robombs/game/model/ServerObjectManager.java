package robombs.game.model;

import java.util.*;

import robombs.clientserver.*;
import robombs.game.*;

/**
 * A manager class that handles local objects on the server identified by a combination of client- and objectID.
 */
public class ServerObjectManager {
    private Map<ClientInfo, LocalObjectList> client2LocalObj=null;

    /**
     * Create a new manager.
     */
    public ServerObjectManager() {
        reset();
    }
    
    public void reset() {
    	NetLogger.log("Server: Resetting the server's object state!");
    	client2LocalObj=new HashMap<ClientInfo, LocalObjectList>();
    }

    /**
     * Gets a local object stored on the server identified by the clientID and the objectID. The actual client don't know a thing
     * about which local object ID their objects will get on the server which is why they can't transmit them. All they know is, how THEY
     * identify their objects. This method maps between those two worlds.
     * @param objID the objectID
     * @param clientID the clientID
     * @return LocalObject the corresponding local object
     */
    public LocalObject getLocalObjectToIDs(int objID, int clientID) {
        for (Iterator<LocalObjectList> itty=client2LocalObj.values().iterator(); itty.hasNext();) {
            LocalObjectList entry=itty.next();
            for (Iterator<LocalObject> itty2=entry.iterator(); itty2.hasNext();) {
                LocalObject lo=itty2.next();
                if (lo.getClientID()==clientID) {
                    if (lo.getObjectID() == objID) {
                        return lo;
                    }
                } else {
                    break; // Wrong client!
                }
            }
        }
        return null;
    }

    /**
     * Fills a data container with data from the all local objects on the server.
     * @param dc the container to fill
     */
    public void fill(ExtendedDataContainer dc, ClientInfo ci) {
    	Map<ClientInfo, LocalObjectList> copy=new HashMap<ClientInfo, LocalObjectList>(client2LocalObj);
    	if (ci!=null) {
    		copy.remove(ci);
    	}
        for (Iterator<LocalObjectList> itty=copy.values().iterator(); itty.hasNext();) {
            LocalObjectList lol=itty.next();
            for (Iterator<LocalObject> itty2=lol.iterator(); itty2.hasNext();) {
                LocalObject lo=itty2.next();
                dc.add(lo);
            }
        }
    }

    /**
     * Takes the input transmitted by a client and creates or updates the local instances that represent
     * the objects on the client.
     * @param ec the container with the client's objects
     */
    public void setOrUpdate(ExtendedDataContainer ec) {
        ClientInfo ci=ec.getClientInfo();
        LocalObjectList lol=getLocalObjectList(ci);
        LocalObject lo=new LocalObject(ci.getID(), true);
        ec.fillLocalObject(lo);
        boolean found=false;
        for (Iterator<LocalObject> itty=lol.iterator(); itty.hasNext();) {
            LocalObject lot=itty.next();
            if (lot.equals(lo)) {
                lot.copyFrom(lo);
                found = true;
                break;
            }
        }
        if (!found) {
            lol.add(lo);
            NetLogger.log("Server: Added an object of client "+ci.getID()+"! Client's object count is now: "+lol.size());
        }
    }

    /**
     * Removes an object from the server's list, i.e. it doesn't exist on the client any longer.
     * @param lo the object to remove
     * @param ci the client info
     */
    public void removeObject(LocalObject lo, ClientInfo ci) {
         LocalObjectList lol=getLocalObjectList(ci);
         boolean ok=lol.remove(lo);
         if (ok) {
             NetLogger.log("Server: Removed an object ("+lo.getObjectID()+") of client " + ci.getID() + "! Client's object count is now: " + lol.size());
         } else {
             NetLogger.log("Server: Failed to remove object of client " + ci);
         }
    }

    /**
     * Returns a list of all local objects for a specific client.
     * @param ci the client for the client in question
     * @return LocalObjectList the list of local objects for this client that the server knows of
     */
    public LocalObjectList getLocalObjectList(ClientInfo ci) {
        if (!client2LocalObj.containsKey(ci)) {
            LocalObjectList lol=new LocalObjectList();
            client2LocalObj.put(ci, lol);
        }
        return (LocalObjectList) client2LocalObj.get(ci);
    }

    /**
     * Removes a client completely from the list, i.e. it kills all of its local objects on the server.
     * @param ci the client
     */
    public void remove(ClientInfo ci) {
        client2LocalObj.remove(ci);
    }
}
