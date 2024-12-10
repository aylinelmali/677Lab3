package cache;

import peer.ReplyStatus;
import product.Product;
import warehouse.IWarehouse;

import java.rmi.RemoteException;

public interface IWarehouseCache {

    static IWarehouseCache getNewWarehouseCache(IWarehouse warehouse) {
        return new FIFOWarehouseCache(warehouse);
    }

    int lookup(Product product) throws RemoteException;

    ReplyStatus buy(UpdateMessage updateMessage) throws RemoteException;

    ReplyStatus sell(UpdateMessage updateMessage) throws RemoteException;

    void updateCache(UpdateMessage cacheUpdateMessage) throws RemoteException;

    int getNextSequenceNumber(int peerID) throws RemoteException;
}
