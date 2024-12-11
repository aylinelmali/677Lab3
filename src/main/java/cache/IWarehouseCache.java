package cache;

import peer.ReplyStatus;
import product.Product;
import warehouse.IWarehouse;

import java.rmi.RemoteException;

public interface IWarehouseCache {

    /**
     * Simplifies the cache creation.
     * @param warehouse The warehouse to be cached.
     * @return The new warehouse cache.
     */
    static IWarehouseCache getNewWarehouseCache(IWarehouse warehouse) {
        return new FIFOWarehouseCache(warehouse);
    }

    /**
     * Used for looking up the amount of product.
     * @param product Product to lookup.
     * @return Returns the amount of product in the inventory.
     */
    int lookup(Product product) throws RemoteException;

    /**
     * Buys a specific amount of products and removes them from the inventory.
     * @param updateMessage Contains important information, such as product type, amount, and metadata for caching.
     * @return Returns the reply status.
     */
    ReplyStatus buy(UpdateMessage updateMessage) throws RemoteException;

    /**
     * Sells a specific amount of items to the warehouse.
     * @param updateMessage Contains important information, such as product type, amount, and metadata for caching.
     * @return Returns the reply status.
     */
    ReplyStatus sell(UpdateMessage updateMessage) throws RemoteException;

    /**
     * Updates the cache data to stay consistent.
     * @param cacheUpdateMessage Contains important information, such as product type, amount, and metadata for caching.
     */
    void updateCache(UpdateMessage cacheUpdateMessage) throws RemoteException;

    /**
     * Used to generate new sequence number for the individual caching approach.
     * @param peerID ID of the peer.
     * @return Returns the sequence number corresponding to the peer ID.
     */
    int getNextSequenceNumber(int peerID) throws RemoteException;
}
