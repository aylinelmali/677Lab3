package peer;

import cache.FIFOWarehouseCache;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import product.Product;
import warehouse.Warehouse;

import java.rmi.RemoteException;

public class CommunicationTest {

    @Test
    public void testSell() throws RemoteException {
        Warehouse warehouse = new Warehouse();
        Seller trader = new Seller(0, new FIFOWarehouseCache(warehouse), 1);
        trader.setPeers(new IPeer[] { trader });
        trader.election(new int[] {}, 1);
        Assertions.assertEquals(0, trader.traderIDs[0]);

        ReplyStatus status = trader.sell(Product.BOARS, 3);
        Assertions.assertEquals(ReplyStatus.SUCCESSFUL, status);
    }

    @Test
    public void testBuySingleTrader() throws RemoteException {
        Warehouse warehouse = new Warehouse();
        Seller trader = new Seller(0, new FIFOWarehouseCache(warehouse), 1);
        trader.setPeers(new IPeer[] { trader });
        trader.election(new int[] {}, 1);
        Assertions.assertEquals(0, trader.traderIDs[0]);

        ReplyStatus sellStatus = trader.sell(Product.BOARS, 3);
        Assertions.assertEquals(ReplyStatus.SUCCESSFUL, sellStatus);

        ReplyStatus buyStatus = trader.buy(Product.BOARS, 3);
        Assertions.assertEquals(ReplyStatus.SUCCESSFUL, buyStatus);

        // test behavior if cache is out of date
        warehouse.sell(Product.BOARS, 3);

        ReplyStatus buyStatus2 = trader.buy(Product.BOARS, 1);
        Assertions.assertEquals(ReplyStatus.UNSUCCESSFUL, buyStatus2);
    }

    @Test
    public void testBuyMultipleTraders() throws RemoteException {
        Warehouse warehouse = new Warehouse();
        Seller trader1 = new Seller(0, new FIFOWarehouseCache(warehouse), 3);
        Seller trader2 = new Seller(1, new FIFOWarehouseCache(warehouse), 3);
        Seller trader3 = new Seller(2, new FIFOWarehouseCache(warehouse), 3);
        trader1.setPeers(new IPeer[] { trader1, trader2, trader3 });
        trader2.setPeers(new IPeer[] { trader1, trader2, trader3 });
        trader3.setPeers(new IPeer[] { trader1, trader2, trader3 });
        trader1.election(new int[] {}, 3);

        Assertions.assertEquals(0, trader1.traderIDs[0]);
        Assertions.assertEquals(1, trader1.traderIDs[1]);
        Assertions.assertEquals(2, trader1.traderIDs[2]);

        ReplyStatus sellStatus = trader1.sell(Product.BOARS, 3);
        Assertions.assertEquals(ReplyStatus.SUCCESSFUL, sellStatus);

        ReplyStatus buyStatus = trader2.buy(Product.BOARS, 3);
        Assertions.assertEquals(ReplyStatus.SUCCESSFUL, buyStatus);

        // test behavior if cache is out of date
        warehouse.sell(Product.BOARS, 3);

        ReplyStatus buyStatus2 = trader3.buy(Product.BOARS, 1);
        Assertions.assertEquals(ReplyStatus.UNSUCCESSFUL, buyStatus2);
    }
}
