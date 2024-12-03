package peer;

import cache.CacheUpdateMessage;
import cache.FIFOWarehouseCache;
import cache.IWarehouseCache;
import product.Product;
import utils.Logger;
import utils.Messages;
import warehouse.Warehouse;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Random;

public abstract class APeer extends UnicastRemoteObject implements IPeer {

    public static final int REGISTRY_PORT = 1099;

    public boolean trader;
    protected int peerID;
    protected int[] traderIDs;
    public IPeer[] peers;
    public int traderPosition;
    protected Warehouse warehouse;
    protected IWarehouseCache warehouseCache;
    protected int sequenceNumber;

    public APeer(int peerID) throws RemoteException, NotBoundException {
        this.trader = false;
        this.peerID = peerID;
        this.traderPosition = new Random().nextInt(0, 2);
        Registry registry = LocateRegistry.getRegistry("127.0.0.1", REGISTRY_PORT);
        this.warehouse = (Warehouse) registry.lookup(Warehouse.WAREHOUSE_NAME);
        this.warehouseCache = new FIFOWarehouseCache(this.warehouse);
        sequenceNumber = 0;
    }

    @Override
    public final void election(int[] tags) throws RemoteException {
        // check if election has reached every peer
        for (int tag : tags) {
            if (tag == peerID) { // election has reached every peer
                int[] max = getTwoLargestPeerIDs(tags); // find max peer
                Logger.log(Messages.getElectionDoneMessage(max), getPeerLogFile());
                coordinator(max, tags);
                return;
            }
        }

        // election has not reached every peer, forward election to next peer that is alive.
        int[] newTags = getNewTags(tags);
        Logger.log(Messages.getPeerDoingElectionMessage(peerID, newTags), getPeerLogFile());
        for (int i = 1; i <= peers.length; i++) {
            int nextPeer = (i + peerID) % peers.length;
            try { // check if next peer is alive, else try next peer.
                peers[nextPeer].election(newTags);
                break;
            } catch (Exception e) {
                Logger.log(Messages.getPeerDoesNotRespondMessage(nextPeer), getPeerLogFile());
            }
        }
    }

    @Override
    public final void coordinator(int[] traderIDs, int[] tags) throws RemoteException {

        // forward coordinator message to next peer in the tags array.
        Logger.log(Messages.getPeerUpdatesCoordinatorMessage(this.peerID, traderIDs), getPeerLogFile());
        this.traderIDs = traderIDs; // update coordinator
        int tagIndex = getPeerTagIndex(tags);
        if (tagIndex != -1 && tagIndex < tags.length-1) {
            peers[tags[tagIndex + 1]].coordinator(traderIDs, tags); // forward message
        }
    }

    @Override
    public synchronized ReplyStatus buy(Product product, int amount) throws RemoteException {
        if (!this.trader) {
            return ReplyStatus.NOT_A_TRADER;
        }

        ReplyStatus replyStatus = this.warehouseCache.buy(product, amount);

        if (replyStatus == ReplyStatus.SUCCESSFUL) {
            this.sequenceNumber++;
            int newStock = this.warehouseCache.lookup(product);
            updateAllTraderCaches(new CacheUpdateMessage(this.sequenceNumber, this.peerID, product, newStock));
        }

        return replyStatus;
    }

    @Override
    public synchronized ReplyStatus sell(Product product, int amount) throws RemoteException {
        if (!this.trader) {
            return ReplyStatus.NOT_A_TRADER;
        }

        ReplyStatus replyStatus = this.warehouseCache.sell(product, amount);

        if (replyStatus == ReplyStatus.SUCCESSFUL) {
            this.sequenceNumber++;
            int newStock = this.warehouseCache.lookup(product);
            updateAllTraderCaches(new CacheUpdateMessage(this.sequenceNumber, this.peerID, product, newStock));
        }

        return replyStatus;
    }

    @Override
    public void updateCache(CacheUpdateMessage cacheUpdateMessage) throws RemoteException {
        warehouseCache.updateCache(cacheUpdateMessage);
    }

    @Override
    public int getPeerID() {
        return this.peerID;
    }

    /**
     * Adds this peer id to tags array.
     * @param tags Old tags array not containing this peer id.
     * @return New tags array containing this peer id.
     */
    protected int[] getNewTags(int[] tags) {
        int[] newSearchPath = new int[tags.length + 1];
        System.arraycopy(tags, 0, newSearchPath, 0, tags.length);
        newSearchPath[tags.length] = peerID;
        return newSearchPath;
    }

    /**
     * Retrieves index of this peer id in tags array.
     * @param tags Tags array containing all peer indices.
     * @return Index of this peer id in tags array.
     */
    private int getPeerTagIndex(int[] tags) {
        for (int i = 0; i < tags.length; i++) {
            if (tags[i] == this.peerID) {
                return i;
            }
        }
        return -1;
    }

    private int[] getTwoLargestPeerIDs(int[] peerIDs) {
        int largestA = Integer.MIN_VALUE, largestB = Integer.MIN_VALUE;

        for (int value : peerIDs) {
            if (value > largestA) {
                largestB = largestA;
                largestA = value;
            } else if (value > largestB) {
                largestB = value;
            }
        }
        return new int[] { largestA, largestB };
    }

    protected String getPeerLogFile() {
        return "peer" + peerID + "_log.txt";
    }

    protected IPeer getCurrentTrader() {
        return this.peers[this.traderIDs[traderPosition]];
    }

    public void setPeers(IPeer[] peers) {
        this.peers = peers;
    }

    public void updateAllTraderCaches(CacheUpdateMessage cacheUpdateMessage) throws RemoteException {
        for (int traderID : this.traderIDs) {
            IPeer peer = this.peers[traderID];
            peer.updateCache(cacheUpdateMessage);
        }
    }
}
