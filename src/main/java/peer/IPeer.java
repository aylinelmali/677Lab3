package peer;

import cache.CacheUpdateMessage;
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
     * @param product Product to buy.
     * @param amount Amount to buy.
     */
    ReplyStatus buy(Product product, int amount) throws RemoteException;

    /**
     * Make a sell request to the warehouse.
     * @param product Product to sell.
     * @param amount Amount to sell.
     */
    ReplyStatus sell(Product product, int amount) throws RemoteException;

    // caching

    /**
     * Update cache of the peer.
     * @param cacheUpdateMessage The update message that contains the data necessary for the update.
     */
    void updateCache(CacheUpdateMessage cacheUpdateMessage) throws RemoteException;

    // getters

    /**
     * @return The ID of the peer.
     */
    int getPeerID() throws RemoteException;

    // heartbeat for fault tolerance

    /**
     * Sends a heartbeat message to check if the peer is alive.
     * @throws RemoteException if the peer is not reachable.
     */
    void heartbeat() throws RemoteException;

    /**
     * Starts heartbeat only in Traders.
     */
    void startHeartbeat() throws RemoteException;
}
