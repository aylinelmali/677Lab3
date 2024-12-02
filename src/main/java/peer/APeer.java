package peer;

import product.Product;
import utils.Logger;
import utils.Messages;
import warehouse.Warehouse;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public abstract class APeer implements IPeer {

    public static final int REGISTRY_PORT = 1099;

    public boolean trader;
    protected int peerID;
    public int[] traderIDs;
    public IPeer[] peers;


    protected Warehouse warehouse;

    public APeer(int peerID) throws RemoteException, NotBoundException {
        this.trader = false;
        this.peerID = peerID;

        Registry registry = LocateRegistry.getRegistry("127.0.0.1", REGISTRY_PORT);
        this.warehouse = (Warehouse) registry.lookup(Warehouse.WAREHOUSE_NAME);
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
    public Status buy(Product product, int amount) throws RemoteException {
        if (!this.trader) {
            return Status.NOT_A_TRADER;
        }
        return this.warehouse.buy(product, amount);
    }

    @Override
    public Status sell(Product product, int amount) throws RemoteException {
        if (!this.trader) {
            return Status.NOT_A_TRADER;
        }
        return warehouse.sell(product, amount);
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

    private String getPeerLogFile() {
        return "peer" + peerID + "_log.txt";
    }

    public void setPeers(IPeer[] peers) {
        this.peers = peers;
    }
}
