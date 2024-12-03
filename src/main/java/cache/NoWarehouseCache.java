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
    public ReplyStatus buy(Product product, int quantity) throws RemoteException {
        return warehouse.buy(product, quantity);
    }

    @Override
    public ReplyStatus sell(Product product, int quantity) throws RemoteException {
        return warehouse.sell(product, quantity);
    }

    @Override
    public void updateCache(CacheUpdateMessage cacheUpdateMessage) {}
}
