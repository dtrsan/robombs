package robombs.game;

import java.util.List;

import robombs.clientserver.*;
import robombs.game.model.*;

/**
 * Interface that defines a game client.
 */
public interface GameClient {

        /**
         * Connect the client to a server
         * @param server ServerEntry the server entry of the server to connect to
         * @param name String the name of this client
         * @throws Exception
         */
        void connect(ServerEntry server, String name) throws Exception;

        /**
         * Disconnect from the current server
         * @throws Exception
         */
        void disconnect() throws Exception;

        /**
         * Checks if the client is connected to a server.
         * @return boolean is it connected or not?
         */
        boolean isConnected();

        /**
         * Returns the server represented by a server entry.
         * @return ServerEntry the server
         */
        ServerEntry getServer();

        /**
         * Starts a local server on the client machine.
         * @param port int the port on which the server should listen
         * @throws Exception
         */
        void startServer(int port) throws Exception;

        /**
         * Shuts down a local server on the client machine if one is running.
         * @throws Exception
         */
        void shutDownServer() throws Exception;

        /**
         * Returns true, is a local server is running on this client machine.
         * @return boolean is it?
         */
        boolean runsServer();
        
        BlueThunderServer getServerImpl();
        
        void ready(boolean isReady);
        
        boolean isReady();
        
        MapList getMapList();
        
        void addBot();
        
        int getMapNumber();
        
        int[] getLocalPlayerInfo();
        
        void sendEvent(Event event);
        
        NetState getState();
        
        void logout(int cid, PlayerInfo player);
        
        List<String> getPlayers();
        
        List<Integer> getTeams();
        
        List<MapInfo> getSelectedMaps();

        int getClientID();
        
        void startSinglePlayer();
        
        /**
         * Shuts down the client and everything...
         */
        void quit();
}
