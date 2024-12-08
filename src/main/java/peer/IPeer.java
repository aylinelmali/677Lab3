package peer;

import cache.UpdateMessage;
import product.Product;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IPeer extends Remote {

    // coordination

    /**
     * Sends an election message to the next peer.
     * @param tags Contains the ID's of the election.
     * @param n Number of traders to elect.
     */
    void election(int[] tags, int n) throws RemoteException;

    /**
     * Sends a coordinator to the next peer to tell the new coordinator.
     * @param traderIDs The id of the new coordinators.
     * @param tags Contains the ID's of the election.
     */
    void coordinator(int[] traderIDs, int[] tags) throws RemoteException;

    // communication

    /**
     * Initializes and starts the peer.
     */
    void start() throws RemoteException;

    /**
     * Make a buy request to the warehouse.
     * @param updateMessage Message to send to the trader. Contains all necessary information for buying.
     */
    ReplyStatus buy(UpdateMessage updateMessage) throws RemoteException;

    /**
     * Make a sell request to the warehouse.
     * @param updateMessage Message to send to the trader. Contains all necessary information for selling.
     */
    ReplyStatus sell(UpdateMessage updateMessage) throws RemoteException;

    // caching

    /**
     * Update cache of the peer.
     * @param cacheUpdateMessage The update message that contains the data necessary for the update.
     */
    void updateCache(UpdateMessage cacheUpdateMessage) throws RemoteException;

    // getters

    /**
     * @return The ID of the peer.
     */
    int getPeerID() throws RemoteException;

    // heartbeat for fault tolerance

    /**
     * Sends a heartbeat message to check if the peer is alive.
     */
    void sendHeartbeat() throws RemoteException;

    /**
     * Respond to a heartbeat message to indicate that the peer is alive.
     */
    void respondToHeartbeat() throws RemoteException;

    /**
     * Starts the heartbeat mechanism.
     */
    void startHeartbeat() throws RemoteException;

    /**
     * Update the trader of the peer.
     * @param traderID ID of the new trader.
     */
    void updateTrader(int traderID) throws RemoteException;

    /**
     * Simulate crash of the peer.
     */
    void crash() throws RemoteException;
}
