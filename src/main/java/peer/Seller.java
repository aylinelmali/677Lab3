package peer;

import cache.IWarehouseCache;
import cache.UpdateMessage;
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
        registry.rebind("" + peerID, new Seller(peerID, IWarehouseCache.getNewWarehouseCache(warehouse), peersAmt));
    }

    // CLASS

    public static final int ACCRUAL_PERIOD = 5000; // Tg: time interval for accruing goods (in ms)
    public static final int GOODS_PER_PERIOD = 5; // Ng: number of goods accrued per period
    public int sellSequenceNumber;

    private Product currentProduct;
    private int inventory; // Seller's local inventory of goods

    public Seller(int peerID, IWarehouseCache warehouseCache, int peersAmt) throws RemoteException {
        super(peerID, warehouseCache, peersAmt);
        this.currentProduct = Product.pickRandomProduct();
        this.inventory = 0;
        this.sellSequenceNumber = 1;
    }

    @Override
    public void start() throws RemoteException {
        super.start();
        Logger.log("Peer " + peerID + " (Seller) started.", getPeerLogFile());
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        // Periodically accrue goods and attempt to sell them
        executor.scheduleAtFixedRate(() -> {
            try {
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
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, ACCRUAL_PERIOD, ACCRUAL_PERIOD, TimeUnit.MILLISECONDS);
    }

    // Add goods to inventory
    private void accrueGoods() {
        inventory += GOODS_PER_PERIOD;
    }

    private void initiateSell() {
        try {
            // Attempt to sell the entire inventory to the current trader
            Logger.log(Messages.getSellAttemptMessage(this.peerID, this.getCurrentTrader().getPeerID(), currentProduct, inventory), getPeerLogFile());
            ReplyStatus status = getCurrentTrader().sell(new UpdateMessage(sellSequenceNumber, peerID, currentProduct, inventory));
            switch (status) {
                case SUCCESSFUL -> {
                    Logger.log(Messages.getSellSuccessfulMessage(this.peerID, this.getCurrentTrader().getPeerID(), currentProduct, inventory), getPeerLogFile());
                    inventory = 0; // Reset inventory after successful sell
                    // Optionally, pick a new product after selling
                    currentProduct = Product.pickRandomProduct();
                    this.sellSequenceNumber++;
                }
                case LOW_SEQUENCE_NUMBER -> { // sequence number too low. Warehouse already updated.
                    Logger.log(Messages.getSellLowSequenceNumberMessage(this.peerID, this.getCurrentTrader().getPeerID(), currentProduct, inventory), getPeerLogFile());
                    inventory = 0; // Reset inventory after successful sell
                    // Optionally, pick a new product after selling
                    currentProduct = Product.pickRandomProduct();
                    this.sellSequenceNumber++;
                }
                // Can only sell to traders
                case NOT_A_TRADER ->
                    Logger.log(Messages.getNotATraderMessage(this.peerID, this.getCurrentTrader().getPeerID()), getPeerLogFile());
                case ERROR_DURING_WRITE ->
                    Logger.log(Messages.getSellErrorMessage(this.peerID, this.getCurrentTrader().getPeerID(), currentProduct, inventory), getPeerLogFile());
            }
        } catch (RemoteException ignored) {}
    }
}
