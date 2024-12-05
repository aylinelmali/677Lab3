package cache;

import peer.ReplyStatus;
import product.Product;
import warehouse.IWarehouse;
import warehouse.Warehouse;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FIFOWarehouseCache implements IWarehouseCache {

    private final IWarehouse warehouse;
    private final Map<Product, Integer> inventoryCache;
    private final List<CacheUpdateMessage> cacheUpdateMessages;
    private final Map<Integer, Integer> peerIDToSequenceNumber;

    public FIFOWarehouseCache(IWarehouse warehouse) {
        this.warehouse = warehouse;
        this.inventoryCache = new HashMap<>();
        this.cacheUpdateMessages = new ArrayList<>();
        this.peerIDToSequenceNumber = new HashMap<>();
    }

    @Override
    public int lookup(Product product) {
        return inventoryCache.getOrDefault(product, 0);
    }

    @Override
    public synchronized ReplyStatus buy(Product product, int quantity) throws RemoteException {
        if (inventoryCache.get(product) == null) {
            inventoryCache.put(product, warehouse.lookup(product));
        }
        if (inventoryCache.get(product) < quantity) {
            return ReplyStatus.UNSUCCESSFUL;
        }

        return warehouse.buy(product, quantity);
    }

    @Override
    public synchronized ReplyStatus sell(Product product, int quantity) throws RemoteException {
        return warehouse.sell(product, quantity);
    }

    @Override
    public synchronized void updateCache(CacheUpdateMessage cacheUpdateMessage) {
        if (peerIDToSequenceNumber.getOrDefault(cacheUpdateMessage.peerID(), 0) == cacheUpdateMessage.sequenceNumber() - 1) {
            update(cacheUpdateMessage);
            boolean found = true;
            while (found) {
                found = false;
                for (CacheUpdateMessage queuedMessage : cacheUpdateMessages) {
                    if (peerIDToSequenceNumber.get(cacheUpdateMessage.peerID()) == queuedMessage.sequenceNumber() - 1) {
                        update(queuedMessage);
                        cacheUpdateMessages.remove(queuedMessage);
                        found = true;
                        break;
                    }
                }
            }

        } else if (peerIDToSequenceNumber.getOrDefault(cacheUpdateMessage.peerID(), 0) < cacheUpdateMessage.sequenceNumber() - 1) {
            cacheUpdateMessages.add(cacheUpdateMessage);
        }
    }

    @Override
    public int getNextSequenceNumber(int peerID) {
        return peerIDToSequenceNumber.getOrDefault(peerID, 0) + 1;
    }

    private void update(CacheUpdateMessage cacheUpdateMessage) {
        int stock = this.inventoryCache.getOrDefault(cacheUpdateMessage.product(), 0);
        this.inventoryCache.put(cacheUpdateMessage.product(), stock + cacheUpdateMessage.amount());
        peerIDToSequenceNumber.put(cacheUpdateMessage.peerID(), cacheUpdateMessage.sequenceNumber());
    }
}
