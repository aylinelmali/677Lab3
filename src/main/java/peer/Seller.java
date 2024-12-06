package peer;

import cache.FIFOWarehouseCache;
import cache.IWarehouseCache;
import product.Product;
import utils.Logger;
import utils.Messages;
import warehouse.IWarehouse;
import warehouse.Warehouse;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Seller extends APeer {

    public static void main(String[] args) throws RemoteException, NotBoundException {
        int peerID = Integer.parseInt(args[0]);
        int peersAmt = Integer.parseInt(args[1]);

        Registry registry = LocateRegistry.getRegistry("127.0.0.1", REGISTRY_PORT);
        IWarehouse warehouse = (IWarehouse) registry.lookup(Warehouse.WAREHOUSE_NAME);
        registry.rebind("" + peerID, new Seller(peerID, new FIFOWarehouseCache(warehouse), peersAmt));
    }

    // CLASS

    public static final int ACCRUAL_PERIOD = 10000; // Tg: time interval for accruing goods (in ms)
    public static final int GOODS_PER_PERIOD = 5; // Ng: number of goods accrued per period
    public static final int MAX_ATTEMPTS = 3;

    private Product currentProduct;
    private int inventory; // Seller's local inventory of goods

    public Seller(int peerID, IWarehouseCache warehouseCache, int peersAmt) throws RemoteException {
        super(peerID, warehouseCache, peersAmt);
        this.currentProduct = Product.pickRandomProduct();
        this.inventory = 0;
    }

    @Override
    public void start() throws RemoteException {
        super.start();
        Logger.log("Peer " + peerID + " (Seller) started.", getPeerLogFile());
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        // Periodically accrue goods and attempt to sell them
        executor.scheduleAtFixedRate(() -> {
            // Skip if this seller is a trader
            if (this.isTrader()) {
                return;
            }

            // Accrue goods
            accrueGoods();

            // Sell goods if inventory > 0
            if (inventory > 0) {
                initiateSell();
            }
        }, ACCRUAL_PERIOD, ACCRUAL_PERIOD, TimeUnit.MILLISECONDS);
    }

    private void accrueGoods() {
        inventory += GOODS_PER_PERIOD;
    }

    private void initiateSell() {
        String transactionID = peerID + "-SELL-" + System.currentTimeMillis();
        try {
            // Attempt to sell the entire inventory to the current trader
            Logger.log(Messages.getSellAttemptMessage(this.peerID, this.getCurrentTrader().getPeerID(), currentProduct, inventory), getPeerLogFile());
            ReplyStatus status = getCurrentTrader().sell(currentProduct, inventory);

            if (status == ReplyStatus.SUCCESSFUL) {
                transactionRetries.remove(transactionID);
                Logger.log(Messages.getSellSuccessfulMessage(this.peerID, this.getCurrentTrader().getPeerID(), currentProduct, inventory), getPeerLogFile());
                inventory = 0; // Reset inventory after successful sell
                // Optionally, pick a new product after selling
                currentProduct = Product.pickRandomProduct();
            } else {
                retryTransaction(transactionID, this::initiateSell, ACCRUAL_PERIOD, MAX_ATTEMPTS);
                Logger.log(Messages.getSellUnsuccessfulMessage(this.peerID, this.getCurrentTrader().getPeerID(), currentProduct, inventory), getPeerLogFile());
            }
        } catch (RemoteException e) {
            retryTransaction(transactionID, this::initiateSell, ACCRUAL_PERIOD, MAX_ATTEMPTS);
            throw new RuntimeException(e);
        }
    }
}
