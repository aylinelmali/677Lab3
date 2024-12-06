package peer;

import cache.CacheUpdateMessage;
import cache.IWarehouseCache;
import product.Product;
import utils.Logger;
import utils.Messages;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public abstract class APeer extends UnicastRemoteObject implements IPeer {

    public static final int REGISTRY_PORT = 1099;
    private static final int HEARTBEAT_INTERVAL = 2000; // in milliseconds
    private static final int HEARTBEAT_TIMEOUT = 4000; // in milliseconds
    private boolean isAlive = true;
    protected final ConcurrentHashMap<String, Integer> transactionRetries = new ConcurrentHashMap<>();
    protected final ScheduledExecutorService retryExecutor = Executors.newScheduledThreadPool(1);

    protected int peerID;
    public int[] traderIDs;
    public IPeer[] peers;
    public int traderPosition;
    protected IWarehouseCache warehouseCache;
    private ScheduledExecutorService heartbeatExecutor;

    public APeer(int peerID, IWarehouseCache warehouseCache, int peersAmt) throws RemoteException {
        this.peerID = peerID;
        this.warehouseCache = warehouseCache;
        this.traderPosition = 0;
        peers = new IPeer[peersAmt];
        heartbeatExecutor = Executors.newScheduledThreadPool(1);
    }

    @Override
    public void start() throws RemoteException {
        // get all other peers from the registry
        Registry registry = LocateRegistry.getRegistry("127.0.0.1", REGISTRY_PORT);
        for (int i = 0; i < this.peers.length; i++) {
            try {
                peers[i] = (IPeer) registry.lookup("" + i);
            } catch (NotBoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public final void election(int[] tags, int n) throws RemoteException {

        // check if election has reached every peer
        for (int tag : tags) {
            if (tag == peerID) { // election has reached every peer

                if (n > tags.length) {
                    throw new IllegalArgumentException("Number of traders is greater than the number of peers.");
                }

                // elect traders
                Arrays.sort(tags);
                int[] traderIDs = Arrays.copyOfRange(tags, tags.length - n, tags.length);

                Logger.log(Messages.getElectionDoneMessage(traderIDs), getPeerLogFile());
                coordinator(traderIDs, tags);
                return;
            }
        }

        // election has not reached every peer, forward election to next peer that is alive.
        int[] newTags = getNewTags(tags);
        Logger.log(Messages.getPeerDoingElectionMessage(peerID, newTags), getPeerLogFile());
        for (int i = 1; i <= peers.length; i++) {
            int nextPeer = (i + peerID) % peers.length;
            try { // check if next peer is alive, else try next peer.
                peers[nextPeer].election(newTags, n);
                break;
            } catch (RemoteException | NullPointerException e) {
                Logger.log(Messages.getPeerDoesNotRespondMessage(nextPeer), getPeerLogFile());
            }
        }
    }

    @Override
    public final void coordinator(int[] traderIDs, int[] tags) throws RemoteException {

        // forward coordinator message to next peer in the tags array.
        Logger.log(Messages.getPeerUpdatesCoordinatorMessage(this.peerID, traderIDs), getPeerLogFile());
        this.traderIDs = traderIDs; // update coordinator
        this.traderPosition = new Random().nextInt(0, traderIDs.length); // update trader position
        int tagIndex = getPeerTagIndex(tags);
        if (tagIndex != -1 && tagIndex < tags.length-1) {
            peers[tags[tagIndex + 1]].coordinator(traderIDs, tags); // forward message
        }
    }

    @Override
    public synchronized ReplyStatus buy(Product product, int amount) throws RemoteException {
        if (!this.isTrader()) {
            return ReplyStatus.NOT_A_TRADER;
        }

        ReplyStatus replyStatus = this.warehouseCache.buy(product, amount);

        if (replyStatus == ReplyStatus.SUCCESSFUL) {
            int sequenceNumber = this.warehouseCache.getNextSequenceNumber(this.peerID);
            updateAllTraderCaches(new CacheUpdateMessage(sequenceNumber, this.peerID, product, -amount));
        }

        return replyStatus;
    }

    @Override
    public synchronized ReplyStatus sell(Product product, int amount) throws RemoteException {
        if (!this.isTrader()) {
            return ReplyStatus.NOT_A_TRADER;
        }

        ReplyStatus replyStatus = this.warehouseCache.sell(product, amount);

        if (replyStatus == ReplyStatus.SUCCESSFUL) {
            int sequenceNumber = this.warehouseCache.getNextSequenceNumber(this.peerID);
            updateAllTraderCaches(new CacheUpdateMessage(sequenceNumber, this.peerID, product, amount));
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

    @Override
    public void heartbeat() throws RemoteException {
        // Heartbeat response
        isAlive = true;
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

    public boolean isTrader() {
        boolean trader = false;

        for (int id : this.traderIDs) {
            if (this.peerID == id) {
                trader = true;
                break;
            }
        }

        return trader;
    }

    // Start heartbeat
    @Override
    public void startHeartbeat() {
        if (!isTrader()) return; // Only traders need heartbeat

        heartbeatExecutor.scheduleAtFixedRate(() -> {
            int partnerTraderID = traderIDs[(traderPosition + 1) % 2];
            try {
                IPeer partnerTrader = peers[partnerTraderID];
                partnerTrader.heartbeat(); // send ping
                isAlive = true; // Reset upon successful ping
            } catch (RemoteException e) {
                Logger.log("Trader " + partnerTraderID + " not responding. Taking over as sole trader.", getPeerLogFile());
                isAlive = false; // Mark the peer as dead
                handleTraderFailure();
            }
        }, HEARTBEAT_INTERVAL, HEARTBEAT_INTERVAL, TimeUnit.MILLISECONDS);
    }

    private void handleTraderFailure() {
        traderIDs = new int[]{peerID}; // Become the sole trader
        traderPosition = 0;

        // Notify all peers about the change
        for (IPeer peer : peers) {
            if (peer != null) {
                try {
                    peer.coordinator(traderIDs, new int[]{peerID});
                } catch (RemoteException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public void simulateFailure() {
        isAlive = false;
        heartbeatExecutor.shutdown();
    }

    public void retryTransaction(String transactionID, Runnable retryTask, int delay, int maxAttempts) {
        int attempts = transactionRetries.getOrDefault(transactionID, 0);
        if (attempts < maxAttempts) {
            retryExecutor.schedule(() -> {
                transactionRetries.put(transactionID, attempts + 1);
                retryTask.run();
            }, delay, TimeUnit.MILLISECONDS);
        } else {
            Logger.log("Transaction " + transactionID + " failed after " + maxAttempts + " attempts.", getPeerLogFile());
            transactionRetries.remove(transactionID);
        }
    }
}
