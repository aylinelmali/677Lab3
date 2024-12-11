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

public class Buyer extends APeer{

    public static void main(String[] args) throws RemoteException, NotBoundException {
        int peerID = Integer.parseInt(args[0]);
        int peersAmt = Integer.parseInt(args[1]);

        Registry registry = LocateRegistry.getRegistry("127.0.0.1", REGISTRY_PORT);
        IWarehouse warehouse = (IWarehouse) registry.lookup(Warehouse.WAREHOUSE_NAME);
        registry.rebind("" + peerID, new Buyer(peerID, IWarehouseCache.getNewWarehouseCache(warehouse), peersAmt));
    }

    // CLASS

    public static final int BUY_PERIOD = 5000;

    public Product product;
    public int amount;
    public boolean bought;
    public int buySequenceNumber;

    public Buyer(int peerID, IWarehouseCache warehouseCache, int peersAmt) throws RemoteException {
        super(peerID, warehouseCache, peersAmt);
        bought = false;
        pickNewProduct();
        buySequenceNumber = 1;
    }

    @Override
    public void start() throws RemoteException {
        super.start();
        Logger.log("Peer " + peerID + " (Buyer) started.", getPeerLogFile());
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

        executor.scheduleAtFixedRate(() -> {
            try {
                // only buy something if not coordinator
                if (this.isTrader()) {
                    return;
                }

                // pick new product when bought
                if (this.bought) {
                    pickNewProduct();
                    this.bought = false;
                }
                initiateBuy();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, BUY_PERIOD, BUY_PERIOD, TimeUnit.MILLISECONDS);
    }

    public void initiateBuy() {
        try {
            // attempt to buy product
            Logger.log(Messages.getBuyAttemptMessage(this.peerID, this.getCurrentTrader().getPeerID(), this.product, this.amount), getPeerLogFile());
            ReplyStatus status = getCurrentTrader().buy(new UpdateMessage(this.buySequenceNumber, this.peerID, this.product, this.amount));

            switch (status) {
                case SUCCESSFUL -> {
                    // reset retries to pick a new product
                    this.bought = true;
                    this.buySequenceNumber++;
                    Logger.log(Messages.getBuySuccessfulMessage(this.peerID, getCurrentTrader().getPeerID(), this.product, this.amount), getPeerLogFile());
                }
                case NOT_IN_STOCK -> // item not in stock
                        Logger.log(Messages.getOutOfStockMessage(this.peerID, getCurrentTrader().getPeerID(), this.product, this.amount), getPeerLogFile());
                case LOW_SEQUENCE_NUMBER -> { // sequence number too low. Warehouse already updated.
                    this.bought = true;
                    this.buySequenceNumber++;
                    Logger.log(Messages.getBuyLowSequenceNumberMessage(this.peerID, getCurrentTrader().getPeerID(), this.product, this.amount), getPeerLogFile());
                }
                case NOT_A_TRADER -> // recipient is not a trader, do logging
                        Logger.log(Messages.getNotATraderMessage(this.peerID, getCurrentTrader().getPeerID()), getPeerLogFile());
                case ERROR_DURING_WRITE -> // error during write to file, do logging
                        Logger.log(Messages.getBuyErrorMessage(this.peerID, getCurrentTrader().getPeerID(), this.product, this.amount), getPeerLogFile());
            }
        } catch (RemoteException ignored) {}
    }

    // Pick new product to buy
    public void pickNewProduct() {
        this.product = Product.pickRandomProduct();
        this.amount = (int) (Math.random() * 5) + 1;
    }
}
