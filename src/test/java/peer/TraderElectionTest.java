package peer;

import cache.IWarehouseCache;
import cache.NoWarehouseCache;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import warehouse.Warehouse;

import java.rmi.RemoteException;

public class TraderElectionTest {

    @Test
    public void electionTestAllTraders() throws RemoteException {
        Warehouse warehouse = new Warehouse();
        IWarehouseCache warehouseCache = new NoWarehouseCache(warehouse);
        APeer peer1 = new Buyer(0, warehouseCache, 4);
        APeer peer2 = new Buyer(1, warehouseCache, 4);
        APeer peer3 = new Buyer(2, warehouseCache, 4);
        APeer peer4 = new Buyer(3, warehouseCache, 4);
        APeer[] peers = {peer1, peer2, peer3, peer4};
        peer1.setPeers(peers);
        peer2.setPeers(peers);
        peer3.setPeers(peers);
        peer4.setPeers(peers);

        Assertions.assertNull(peer1.traderIDs);
        Assertions.assertNull(peer2.traderIDs);
        Assertions.assertNull(peer3.traderIDs);
        Assertions.assertNull(peer4.traderIDs);

        peer1.election(new int[] {}, 4);

        Assertions.assertArrayEquals(new int[] {0, 1, 2, 3}, peer1.traderIDs);
        Assertions.assertArrayEquals(new int[] {0, 1, 2, 3}, peer2.traderIDs);
        Assertions.assertArrayEquals(new int[] {0, 1, 2, 3}, peer3.traderIDs);
        Assertions.assertArrayEquals(new int[] {0, 1, 2, 3}, peer4.traderIDs);
    }

    @Test
    public void electionTestOneTraders() throws RemoteException {
        Warehouse warehouse = new Warehouse();
        IWarehouseCache warehouseCache = new NoWarehouseCache(warehouse);
        APeer peer1 = new Buyer(0, warehouseCache, 4);
        APeer peer2 = new Buyer(1, warehouseCache, 4);
        APeer peer3 = new Buyer(2, warehouseCache, 4);
        APeer peer4 = new Buyer(3, warehouseCache, 4);
        APeer[] peers = {peer1, peer2, peer3, peer4};
        peer1.setPeers(peers);
        peer2.setPeers(peers);
        peer3.setPeers(peers);
        peer4.setPeers(peers);

        Assertions.assertNull(peer1.traderIDs);
        Assertions.assertNull(peer2.traderIDs);
        Assertions.assertNull(peer3.traderIDs);
        Assertions.assertNull(peer4.traderIDs);

        peer1.election(new int[] {}, 1);

        Assertions.assertArrayEquals(new int[] {3}, peer1.traderIDs);
        Assertions.assertArrayEquals(new int[] {3}, peer2.traderIDs);
        Assertions.assertArrayEquals(new int[] {3}, peer3.traderIDs);
        Assertions.assertArrayEquals(new int[] {3}, peer4.traderIDs);
    }

    @Test
    public void electionTestTooManyTraders() throws RemoteException {
        Warehouse warehouse = new Warehouse();
        IWarehouseCache warehouseCache = new NoWarehouseCache(warehouse);
        APeer peer1 = new Buyer(0, warehouseCache, 4);
        APeer peer2 = new Buyer(1, warehouseCache, 4);
        APeer peer3 = new Buyer(2, warehouseCache, 4);
        APeer peer4 = new Buyer(3, warehouseCache, 4);
        APeer[] peers = {peer1, peer2, peer3, peer4};
        peer1.setPeers(peers);
        peer2.setPeers(peers);
        peer3.setPeers(peers);
        peer4.setPeers(peers);

        Assertions.assertNull(peer1.traderIDs);
        Assertions.assertNull(peer2.traderIDs);
        Assertions.assertNull(peer3.traderIDs);
        Assertions.assertNull(peer4.traderIDs);

        Assertions.assertThrows(IllegalArgumentException.class, () -> peer1.election(new int[] {}, 5));
    }

    @Test
    public void concurrentElectionTest() throws RemoteException, InterruptedException {
        Warehouse warehouse = new Warehouse();
        IWarehouseCache warehouseCache = new NoWarehouseCache(warehouse);
        APeer peer1 = new Buyer(0, warehouseCache, 4);
        APeer peer2 = new Buyer(1, warehouseCache, 4);
        APeer peer3 = new Buyer(2, warehouseCache, 4);
        APeer peer4 = new Buyer(3, warehouseCache, 4);

        APeer[] peers = {peer1, peer2, peer3, peer4};
        peer1.setPeers(peers);
        peer2.setPeers(peers);
        peer3.setPeers(peers);
        peer4.setPeers(peers);

        Assertions.assertNull(peer1.traderIDs);
        Assertions.assertNull(peer2.traderIDs);
        Assertions.assertNull(peer3.traderIDs);
        Assertions.assertNull(peer4.traderIDs);

        peer1.election(new int[] {}, 2);
        peer3.election(new int[] {}, 2);

        Assertions.assertArrayEquals(new int[] {2, 3}, peer1.traderIDs);
        Assertions.assertArrayEquals(new int[] {2, 3}, peer2.traderIDs);
        Assertions.assertArrayEquals(new int[] {2, 3}, peer3.traderIDs);
        Assertions.assertArrayEquals(new int[] {2, 3}, peer4.traderIDs);
    }

    @Test
    public void reelectionTest() throws RemoteException, InterruptedException {

        Warehouse warehouse = new Warehouse();
        IWarehouseCache warehouseCache = new NoWarehouseCache(warehouse);
        APeer peer1 = new Buyer(0, warehouseCache, 2);
        APeer peer3 = new Buyer(2, warehouseCache, 2);

        APeer[] peers = {peer1, null, peer3, null};
        peer1.setPeers(peers);
        peer3.setPeers(peers);

        Assertions.assertNull(peer1.traderIDs);
        Assertions.assertNull(peer3.traderIDs);

        peer1.election(new int[] {}, 1);

        Assertions.assertArrayEquals(new int[] {2}, peer1.traderIDs);
        Assertions.assertArrayEquals(new int[] {2}, peer3.traderIDs);
    }
}
