package cache;

import peer.ReplyStatus;
import product.Product;
import warehouse.Warehouse;

import java.rmi.RemoteException;

public interface IWarehouseCache {

    static IWarehouseCache getNewWarehouseCache(Warehouse warehouse) {
        return new FIFOWarehouseCache(warehouse);
    }

    int lookup(Product product) throws RemoteException;

    ReplyStatus buy(Product product, int quantity) throws RemoteException;

    ReplyStatus sell(Product product, int quantity) throws RemoteException;

    void updateCache(CacheUpdateMessage cacheUpdateMessage) throws RemoteException;

    int getNextSequenceNumber(int peerID) throws RemoteException;
}
