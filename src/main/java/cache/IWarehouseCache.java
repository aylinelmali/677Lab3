package cache;

import peer.ReplyStatus;
import product.Product;

import java.rmi.RemoteException;

public interface IWarehouseCache {

    int lookup(Product product) throws RemoteException;

    ReplyStatus buy(Product product, int quantity) throws RemoteException;

    ReplyStatus sell(Product product, int quantity) throws RemoteException;

    void updateCache(CacheUpdateMessage cacheUpdateMessage) throws RemoteException;
}
