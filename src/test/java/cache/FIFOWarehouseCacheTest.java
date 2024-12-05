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

        ReplyStatus buyStatus = cache.buy(Product.BOARS, 3);
        Assertions.assertEquals(ReplyStatus.UNSUCCESSFUL, buyStatus);

        ReplyStatus sellStatus = cache.sell(Product.BOARS, 3);
        Assertions.assertEquals(ReplyStatus.SUCCESSFUL, sellStatus);

        ReplyStatus buyStatus2 = cache.buy(Product.BOARS, 3);
        Assertions.assertEquals(ReplyStatus.UNSUCCESSFUL, buyStatus2);
    }

    @Test
    public void buyTest() throws RemoteException {
        Warehouse warehouse = new Warehouse();
        IWarehouseCache cache = new FIFOWarehouseCache(warehouse);

        ReplyStatus buyStatus = cache.buy(Product.BOARS, 3);
        Assertions.assertEquals(ReplyStatus.UNSUCCESSFUL, buyStatus);

        warehouse.sell(Product.BOARS, 3);
        cache.updateCache(new CacheUpdateMessage(1, 0, Product.BOARS, 3));

        ReplyStatus buyStatus2 = cache.buy(Product.BOARS, 3);
        Assertions.assertEquals(ReplyStatus.SUCCESSFUL, buyStatus2);

        ReplyStatus buyStatus3 = cache.buy(Product.BOARS, 1);
        Assertions.assertEquals(ReplyStatus.UNSUCCESSFUL, buyStatus3);
    }

    @Test
    public void FIFOFunctionalityTest() throws RemoteException {
        Warehouse warehouse = new Warehouse();
        IWarehouseCache cache = new FIFOWarehouseCache(warehouse);

        cache.updateCache(new CacheUpdateMessage(1, 0, Product.BOARS, 1));
        Assertions.assertEquals(1, cache.lookup(Product.BOARS));

        cache.updateCache(new CacheUpdateMessage(3, 0, Product.BOARS, 1));
        cache.updateCache(new CacheUpdateMessage(4, 0, Product.BOARS, 1));
        cache.updateCache(new CacheUpdateMessage(6, 0, Product.BOARS, 1));

        Assertions.assertEquals(1, cache.lookup(Product.BOARS));

        cache.updateCache(new CacheUpdateMessage(2, 0, Product.BOARS, 1));

        Assertions.assertEquals(4, cache.lookup(Product.BOARS));
    }
}
