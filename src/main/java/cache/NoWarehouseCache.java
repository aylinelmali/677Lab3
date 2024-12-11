package cache;

import peer.ReplyStatus;
import product.Product;
import warehouse.IWarehouse;
import warehouse.Warehouse;

import java.rmi.RemoteException;

/**
 * Implements no caching for the warehouse.
 */
public class NoWarehouseCache implements IWarehouseCache {

    private final IWarehouse warehouse;

    public NoWarehouseCache(IWarehouse warehouse) {
        this.warehouse = warehouse;
    }

    @Override
    public synchronized int lookup(Product product) throws RemoteException {
        return warehouse.lookup(product);
    }

    @Override
    public synchronized ReplyStatus buy(UpdateMessage updateMessage) throws RemoteException {
        return warehouse.buy(updateMessage);
    }

    @Override
    public synchronized ReplyStatus sell(UpdateMessage updateMessage) throws RemoteException {
        return warehouse.sell(updateMessage);
    }

    @Override
    public synchronized void updateCache(UpdateMessage cacheUpdateMessage) {}

    @Override
    public synchronized int getNextSequenceNumber(int peerID) {
        return 0;
    }
}
