package cache;

import peer.ReplyStatus;
import product.Product;
import warehouse.IWarehouse;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implements caching with FIFO consistency.
 */
public class FIFOWarehouseCache implements IWarehouseCache {

    private final IWarehouse warehouse;
    private final Map<Product, Integer> inventoryCache;
    private final List<UpdateMessage> cacheUpdateMessages;
    private final Map<Integer, Integer> peerIDToSequenceNumber;

    public FIFOWarehouseCache(IWarehouse warehouse) {
        this.warehouse = warehouse;
        this.inventoryCache = new HashMap<>();
        this.cacheUpdateMessages = new ArrayList<>();
        this.peerIDToSequenceNumber = new HashMap<>();
    }

    @Override
    public synchronized int lookup(Product product) {
        return inventoryCache.getOrDefault(product, 0);
    }

    @Override
    public synchronized ReplyStatus buy(UpdateMessage updateMessage) throws RemoteException {
        // check if entry exists in cache, else lookup in warehouse.
        if (inventoryCache.get(updateMessage.product()) == null) {
            inventoryCache.put(updateMessage.product(), warehouse.lookup(updateMessage.product()));
        }

        // check cache if in stock or not.
        if (inventoryCache.get(updateMessage.product()) < updateMessage.amount()) {
            return ReplyStatus.NOT_IN_STOCK;
        }

        return warehouse.buy(updateMessage); // buy from warehouse.
    }

    @Override
    public synchronized ReplyStatus sell(UpdateMessage updateMessage) throws RemoteException {
        return warehouse.sell(updateMessage); // sell to warehouse
    }

    @Override
    public synchronized void updateCache(UpdateMessage cacheUpdateMessage) {
        // check if sequence number is correct. If so, proceed, else put message into queue.
        if (peerIDToSequenceNumber.getOrDefault(cacheUpdateMessage.peerID(), 0) == cacheUpdateMessage.sequenceNumber() - 1) {
            update(cacheUpdateMessage); // update warehouse.

            // check queue if updates can now be applied. If so, apply.
            // this ensures correct update ordering per peer id.
            boolean found = true;
            while (found) {
                found = false;
                for (UpdateMessage queuedMessage : cacheUpdateMessages) {
                    if (peerIDToSequenceNumber.get(cacheUpdateMessage.peerID()) == queuedMessage.sequenceNumber() - 1) {
                        update(queuedMessage);
                        cacheUpdateMessages.remove(queuedMessage);
                        found = true;
                        break;
                    }
                }
            }

        } else if (peerIDToSequenceNumber.getOrDefault(cacheUpdateMessage.peerID(), 0) < cacheUpdateMessage.sequenceNumber() - 1) {
            cacheUpdateMessages.add(cacheUpdateMessage); // put message into queue if sequence number too large.
        }
    }

    @Override
    public int getNextSequenceNumber(int peerID) {
        return peerIDToSequenceNumber.getOrDefault(peerID, 0) + 1; // increment sequence number of peer ID.
    }

    // update warehouse cache and sequence numbers
    private void update(UpdateMessage cacheUpdateMessage) {
        int stock = this.inventoryCache.getOrDefault(cacheUpdateMessage.product(), 0);
        this.inventoryCache.put(cacheUpdateMessage.product(), stock + cacheUpdateMessage.amount());
        peerIDToSequenceNumber.put(cacheUpdateMessage.peerID(), cacheUpdateMessage.sequenceNumber());
    }
}
