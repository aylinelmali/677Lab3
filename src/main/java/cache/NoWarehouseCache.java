package cache;

import peer.ReplyStatus;
import product.Product;
import warehouse.Warehouse;

import java.rmi.RemoteException;

public class NoWarehouseCache implements IWarehouseCache {

    private Warehouse warehouse;

    public NoWarehouseCache(Warehouse warehouse) {
        this.warehouse = warehouse;
    }

    @Override
    public int lookup(Product product) throws RemoteException {
        return warehouse.lookup(product);
    }

    @Override
    public ReplyStatus buy(UpdateMessage updateMessage) throws RemoteException {
        return warehouse.buy(updateMessage);
    }

    @Override
    public ReplyStatus sell(UpdateMessage updateMessage) throws RemoteException {
        return warehouse.sell(updateMessage);
    }

    @Override
    public void updateCache(UpdateMessage cacheUpdateMessage) {}

    @Override
    public int getNextSequenceNumber(int peerID) throws RemoteException {
        return 0;
    }
}
