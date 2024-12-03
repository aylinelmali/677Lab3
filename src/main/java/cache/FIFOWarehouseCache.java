package cache;

import peer.ReplyStatus;
import product.Product;
import warehouse.Warehouse;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FIFOWarehouseCache implements IWarehouseCache {

    private final Warehouse warehouse;
    private final Map<Product, Integer> inventoryCache;
    private final List<CacheUpdateMessage> cacheUpdateMessages;
    private final Map<Integer, Integer> peerIDToSequenceNumber;

    public FIFOWarehouseCache(Warehouse warehouse) {
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
        if (peerIDToSequenceNumber.get(cacheUpdateMessage.peerID()) == cacheUpdateMessage.sequenceNumber() - 1) {
            update(cacheUpdateMessage);
            boolean found = true;
            while (found) {
                found = false;
                for (CacheUpdateMessage queuedMessage : cacheUpdateMessages) {
                    if (peerIDToSequenceNumber.get(cacheUpdateMessage.peerID()) == queuedMessage.sequenceNumber() - 1) {
                        update(cacheUpdateMessage);
                        cacheUpdateMessages.remove(queuedMessage);
                        found = true;
                        break;
                    }
                }
            }

        } else if (peerIDToSequenceNumber.get(cacheUpdateMessage.peerID()) < cacheUpdateMessage.sequenceNumber() - 1) {
            cacheUpdateMessages.add(cacheUpdateMessage);
        }
    }

    private void update(CacheUpdateMessage cacheUpdateMessage) {
        this.inventoryCache.put(cacheUpdateMessage.product(), cacheUpdateMessage.stock());
        peerIDToSequenceNumber.put(cacheUpdateMessage.peerID(), cacheUpdateMessage.sequenceNumber());
    }
}
