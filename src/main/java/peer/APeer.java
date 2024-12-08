package peer;

import cache.UpdateMessage;
import cache.IWarehouseCache;
import utils.Logger;
import utils.Messages;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public abstract class APeer extends UnicastRemoteObject implements IPeer {

    public static final int REGISTRY_PORT = 1099;
    private static final int HEARTBEAT_INTERVAL = 5000; // in milliseconds
    private static final int HEARTBEAT_TIMEOUT = 10500; // in milliseconds

    protected int peerID;
    public int[] traderIDs;
    public IPeer[] peers;
    public int traderPosition;
    protected IWarehouseCache warehouseCache;
    private boolean receivedHeartbeatResponse;
    private boolean crashed;

    public APeer(int peerID, IWarehouseCache warehouseCache, int peersAmt) throws RemoteException {
        this.peerID = peerID;
        this.warehouseCache = warehouseCache;
        this.traderPosition = 0;
        peers = new IPeer[peersAmt];
        receivedHeartbeatResponse = true;
        crashed = false;
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

        // crash simulation
        if (crashed) {
            throw new RemoteException();
        }

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

        // crash simulation
        if (crashed) {
            throw new RemoteException();
        }

        // forward coordinator message to next peer in the tags array.
        Logger.log(Messages.getPeerUpdatesCoordinatorMessage(this.peerID, traderIDs), getPeerLogFile());
        this.traderIDs = traderIDs; // update coordinator
        this.traderPosition = (peerID / 2) % traderIDs.length; // update trader position
        int tagIndex = getPeerTagIndex(tags);
        if (tagIndex != -1 && tagIndex < tags.length-1) {
            peers[tags[tagIndex + 1]].coordinator(traderIDs, tags); // forward message
        }
    }

    @Override
    public synchronized ReplyStatus buy(UpdateMessage updateMessage) throws RemoteException {

        // crash simulation
        if (crashed) {
            throw new RemoteException();
        }

        if (!this.isTrader()) {
            return ReplyStatus.NOT_A_TRADER;
        }

        ReplyStatus replyStatus = this.warehouseCache.buy(updateMessage);

        if (replyStatus == ReplyStatus.SUCCESSFUL) {
            int sequenceNumber = this.warehouseCache.getNextSequenceNumber(this.peerID);
            updateAllTraderCaches(new UpdateMessage(sequenceNumber, this.peerID, updateMessage.product(), -updateMessage.amount()));
        }

        return replyStatus;
    }

    @Override
    public synchronized ReplyStatus sell(UpdateMessage updateMessage) throws RemoteException {

        // crash simulation
        if (crashed) {
            throw new RemoteException();
        }

        if (!this.isTrader()) {
            return ReplyStatus.NOT_A_TRADER;
        }

        ReplyStatus replyStatus = this.warehouseCache.sell(updateMessage);

        if (replyStatus == ReplyStatus.SUCCESSFUL) {
            int sequenceNumber = this.warehouseCache.getNextSequenceNumber(this.peerID);
            updateAllTraderCaches(new UpdateMessage(sequenceNumber, this.peerID, updateMessage.product(), updateMessage.amount()));
        }

        return replyStatus;
    }

    @Override
    public void updateCache(UpdateMessage cacheUpdateMessage) throws RemoteException {

        // crash simulation
        if (crashed) {
            throw new RemoteException();
        }

        warehouseCache.updateCache(cacheUpdateMessage);
    }

    @Override
    public int getPeerID() throws RemoteException {

        // crash simulation
        if (crashed) {
            throw new RemoteException();
        }

        return this.peerID;
    }

    @Override
    public void sendHeartbeat() throws RemoteException {

        // crash simulation
        if (crashed) {
            throw new RemoteException();
        }

        Logger.log(Messages.getSendHeartbeatMessage(this.peerID, this.getOtherTraderID()), this.getPeerLogFile());
        this.peers[getOtherTraderID()].respondToHeartbeat();
    }

    @Override
    public void respondToHeartbeat() throws RemoteException {

        // crash simulation
        if (crashed) {
            throw new RemoteException();
        }

        Logger.log(Messages.getHeartbeatResponseMessage(this.peerID, this.getOtherTraderID()), this.getPeerLogFile());
        this.receivedHeartbeatResponse = true;
    }

    @Override
    public void startHeartbeat() throws RemoteException {

        // crash simulation
        if (crashed) {
            throw new RemoteException();
        }

        if (!isTrader()) {
            return;
        }

        ScheduledExecutorService heartbeatExecutor = Executors.newScheduledThreadPool(1);
        ScheduledExecutorService heartbeatTimeoutExecutor = Executors.newScheduledThreadPool(1);

        heartbeatExecutor.scheduleAtFixedRate(() -> {

            // crash simulation
            if (crashed) {
                heartbeatExecutor.shutdown();
                heartbeatTimeoutExecutor.shutdown();
                return;
            }

            try {
                this.peers[getOtherTraderID()].sendHeartbeat();
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        }, HEARTBEAT_INTERVAL, HEARTBEAT_INTERVAL, TimeUnit.MILLISECONDS);

        heartbeatTimeoutExecutor.scheduleAtFixedRate(() -> {

            // crash simulation
            if (crashed) {
                heartbeatExecutor.shutdown();
                heartbeatTimeoutExecutor.shutdown();
                return;
            }

            if (this.receivedHeartbeatResponse) {
                // do nothing because other is alive
                this.receivedHeartbeatResponse = false;
            } else {
                // other is not alive, send change trader message to all peers.

                Logger.log(Messages.getHeartbeatTimeoutMessage(this.peerID, getOtherTraderID()), getPeerLogFile());
                sendUpdateTraderMessage();
                heartbeatExecutor.shutdown();
                heartbeatTimeoutExecutor.shutdown();
            }
        }, HEARTBEAT_TIMEOUT, HEARTBEAT_TIMEOUT, TimeUnit.MILLISECONDS);
    }

    @Override
    public void updateTrader(int traderID) throws RemoteException {

        // crash simulation
        if (crashed) {
            throw new RemoteException();
        }

        for (int i = 0; i < this.traderIDs.length; i++) {
            if (this.traderIDs[i] == traderID) {
                Logger.log(Messages.getTraderUpdatedMessage(this.peerID, traderID), getPeerLogFile());
                this.traderPosition = i;
                break;
            }
        }
    }

    @Override
    public void crash() throws RemoteException {

        // crash simulation
        if (crashed) {
            throw new RemoteException();
        }

        crashed = true;
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

    protected String getPeerLogFile() {
        return "peer" + peerID + "_log.txt";
    }

    protected IPeer getCurrentTrader() {
        return this.peers[this.traderIDs[traderPosition]];
    }

    public void setPeers(IPeer[] peers) {
        this.peers = peers;
    }

    public void updateAllTraderCaches(UpdateMessage cacheUpdateMessage) {
        for (int traderID : this.traderIDs) {
            IPeer peer = this.peers[traderID];
            try {
                peer.updateCache(cacheUpdateMessage);
            } catch (RemoteException ignored) {}
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

    private int getOtherTraderID() {
        if (this.traderIDs.length < 2) {
            throw new IllegalStateException("This is the only trader.");
        }
        return this.peerID == this.traderIDs[0] ? this.traderIDs[1] : this.traderIDs[0];
    }

    private void sendUpdateTraderMessage() {
        for (IPeer peer : peers) {
            try {
                if (peer.getPeerID() == this.peerID) {
                    continue;
                }
                peer.updateTrader(this.peerID);
            } catch (RemoteException ignored) {}
        }
    }
}
