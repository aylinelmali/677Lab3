package peer;

import product.Product;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IPeer extends Remote {

    // coordination

    /**
     * Sends an election message to the next peer.
     * @param tags Contains the ID's of the election.
     */
    void election(int[] tags) throws RemoteException;

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
    Status buy(Product product, int amount) throws RemoteException;

    /**
     * Make a sell request to the warehouse.
     * @param product Product to sell.
     * @param amount Amount to sell.
     */
    Status sell(Product product, int amount) throws RemoteException;

    /**
     * @return The ID of the peer.
     */
    int getPeerID() throws RemoteException;
}