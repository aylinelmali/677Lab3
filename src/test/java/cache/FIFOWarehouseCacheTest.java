package cache;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import peer.ReplyStatus;
import product.Product;
import warehouse.Warehouse;

import java.rmi.RemoteException;

public class FIFOWarehouseCacheTest {

    @Test
    public void sellTest() throws RemoteException {
        Warehouse warehouse = new Warehouse();
        IWarehouseCache cache = new FIFOWarehouseCache(warehouse);
        ReplyStatus buyStatus = cache.buy(new UpdateMessage(1, 1, Product.BOARS, 3));
        Assertions.assertEquals(ReplyStatus.NOT_IN_STOCK, buyStatus);

        ReplyStatus sellStatus = cache.sell(new UpdateMessage(1, 0, Product.BOARS, 3));
        Assertions.assertEquals(ReplyStatus.SUCCESSFUL, sellStatus);

        ReplyStatus buyStatus2 = cache.buy(new UpdateMessage(1, 1, Product.BOARS, 3));
        Assertions.assertEquals(ReplyStatus.NOT_IN_STOCK, buyStatus2);
    }

    @Test
    public void buyTest() throws RemoteException {
        Warehouse warehouse = new Warehouse();
        IWarehouseCache cache = new FIFOWarehouseCache(warehouse);

        ReplyStatus buyStatus = cache.buy(new UpdateMessage(1, 1, Product.BOARS, 3));
        Assertions.assertEquals(ReplyStatus.NOT_IN_STOCK, buyStatus);

        warehouse.sell(new UpdateMessage(1, 0, Product.BOARS, 3));
        cache.updateCache(new UpdateMessage(1, 0, Product.BOARS, 3));

        ReplyStatus buyStatus2 = cache.buy(new UpdateMessage(2, 1, Product.BOARS, 3));
        Assertions.assertEquals(ReplyStatus.SUCCESSFUL, buyStatus2);

        ReplyStatus buyStatus3 = cache.buy(new UpdateMessage(3, 1, Product.BOARS, 1));
        Assertions.assertEquals(ReplyStatus.NOT_IN_STOCK, buyStatus3);
    }

    @Test
    public void FIFOFunctionalityTest() throws RemoteException {
        Warehouse warehouse = new Warehouse();
        IWarehouseCache cache = new FIFOWarehouseCache(warehouse);

        cache.updateCache(new UpdateMessage(1, 0, Product.BOARS, 1));
        Assertions.assertEquals(1, cache.lookup(Product.BOARS));

        cache.updateCache(new UpdateMessage(3, 0, Product.BOARS, 1));
        cache.updateCache(new UpdateMessage(4, 0, Product.BOARS, 1));
        cache.updateCache(new UpdateMessage(6, 0, Product.BOARS, 1));

        Assertions.assertEquals(1, cache.lookup(Product.BOARS));

        cache.updateCache(new UpdateMessage(2, 0, Product.BOARS, 1));

        Assertions.assertEquals(4, cache.lookup(Product.BOARS));
    }
}
