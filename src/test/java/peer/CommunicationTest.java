package peer;

import cache.FIFOWarehouseCache;
import cache.UpdateMessage;
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

        ReplyStatus status = trader.sell(new UpdateMessage(1, 0, Product.BOARS, 3));
        Assertions.assertEquals(ReplyStatus.SUCCESSFUL, status);
    }

    @Test
    public void testBuySingleTrader() throws RemoteException {
        Warehouse warehouse = new Warehouse();
        Seller trader = new Seller(0, new FIFOWarehouseCache(warehouse), 1);
        trader.setPeers(new IPeer[] { trader });
        trader.election(new int[] {}, 1);
        Assertions.assertEquals(0, trader.traderIDs[0]);

        ReplyStatus sellStatus = trader.sell(new UpdateMessage(1, 0, Product.BOARS, 3));
        Assertions.assertEquals(ReplyStatus.SUCCESSFUL, sellStatus);

        ReplyStatus buyStatus = trader.buy(new UpdateMessage(1, 1, Product.BOARS, 3));
        Assertions.assertEquals(ReplyStatus.SUCCESSFUL, buyStatus);

        // test behavior if cache is out of date
        warehouse.sell(new UpdateMessage(2, 0, Product.BOARS, 3));

        ReplyStatus buyStatus2 = trader.buy(new UpdateMessage(2, 1, Product.BOARS, 1));
        Assertions.assertEquals(ReplyStatus.NOT_IN_STOCK, buyStatus2);
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

        ReplyStatus sellStatus = trader1.sell(new UpdateMessage(1, 0, Product.BOARS, 3));
        Assertions.assertEquals(ReplyStatus.SUCCESSFUL, sellStatus);

        ReplyStatus buyStatus = trader2.buy(new UpdateMessage(1, 1, Product.BOARS, 3));
        Assertions.assertEquals(ReplyStatus.SUCCESSFUL, buyStatus);

        // test behavior if cache is out of date
        warehouse.sell(new UpdateMessage(2, 0, Product.BOARS, 3));

        ReplyStatus buyStatus2 = trader3.buy(new UpdateMessage(2, 1, Product.BOARS, 1));
        Assertions.assertEquals(ReplyStatus.NOT_IN_STOCK, buyStatus2);
    }

    @Test
    public void testWarehouseSequenceNumber() throws RemoteException {
        Warehouse warehouse = new Warehouse();
        Assertions.assertEquals(ReplyStatus.SUCCESSFUL, warehouse.sell(new UpdateMessage(1, 0, Product.BOARS, 1)));
        Assertions.assertEquals(ReplyStatus.LOW_SEQUENCE_NUMBER, warehouse.sell(new UpdateMessage(1, 0, Product.BOARS, 1)));
        Assertions.assertEquals(ReplyStatus.SUCCESSFUL, warehouse.sell(new UpdateMessage(2, 0, Product.BOARS, 1)));

        Assertions.assertEquals(ReplyStatus.SUCCESSFUL, warehouse.buy(new UpdateMessage(1, 1, Product.BOARS, 1)));
        Assertions.assertEquals(ReplyStatus.LOW_SEQUENCE_NUMBER, warehouse.buy(new UpdateMessage(1, 1, Product.BOARS, 1)));
        Assertions.assertEquals(ReplyStatus.SUCCESSFUL, warehouse.buy(new UpdateMessage(2, 1, Product.BOARS, 1)));
    }
}
