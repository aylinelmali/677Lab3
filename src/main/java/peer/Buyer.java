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

public class Buyer extends APeer{

    public static void main(String[] args) throws RemoteException, NotBoundException {
        int peerID = Integer.parseInt(args[0]);
        int peersAmt = Integer.parseInt(args[1]);

        Registry registry = LocateRegistry.getRegistry("127.0.0.1", REGISTRY_PORT);
        IWarehouse warehouse = (IWarehouse) registry.lookup(Warehouse.WAREHOUSE_NAME);
        registry.rebind("" + peerID, new Buyer(peerID, new FIFOWarehouseCache(warehouse), peersAmt));
    }

    // CLASS

    public static final int BUY_PERIOD = 5000;
    public static final int MAX_ATTEMPTS = 3;

    public int retries;
    public Product product;
    public int amount;

    public Buyer(int peerID, IWarehouseCache warehouseCache, int peersAmt) throws RemoteException {
        super(peerID, warehouseCache, peersAmt);
        retries = 0;
    }

    @Override
    public void start() throws RemoteException {
        super.start();
        Logger.log("Peer " + peerID + " (Buyer) started.", getPeerLogFile());
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

        executor.scheduleAtFixedRate(() -> {
            // only buy something if not coordinator
            if (this.isTrader()) {
                return;
            }

            // buy new product when retries equals 0
            if (this.retries == 0 || this.retries >= MAX_ATTEMPTS) {
                this.product = Product.pickRandomProduct();
                this.amount = (int) (Math.random() * 5) + 1;
            }

            initiateBuy();

        }, BUY_PERIOD, BUY_PERIOD, TimeUnit.MILLISECONDS);
    }

    public void initiateBuy() {
        String transactionID = peerID + "-BUY-" + System.currentTimeMillis();
        try {
            // attempt to buy product
            Logger.log(Messages.getBuyAttemptMessage(this.peerID, this.getCurrentTrader().getPeerID(), this.product, this.amount), getPeerLogFile());
            ReplyStatus status = getCurrentTrader().buy(this.product, this.amount);

            switch (status) {
                case SUCCESSFUL -> {
                    // reset retries to pick a new product
                    transactionRetries.remove(transactionID);
                    this.retries = 0;
                    Logger.log(Messages.getBuySuccessfulMessage(this.peerID, getCurrentTrader().getPeerID(), this.product, this.amount), getPeerLogFile());
                }
                case UNSUCCESSFUL -> {
                    // buy unsuccessful, increment reset counter
                    this.retries++;
                    Logger.log(Messages.getBuyUnsuccessfulMessage(this.peerID, getCurrentTrader().getPeerID(), this.product, this.amount, this.retries < MAX_ATTEMPTS), getPeerLogFile());
                    retryTransaction(transactionID, this::initiateBuy, BUY_PERIOD, MAX_ATTEMPTS);
                }
                case NOT_A_TRADER -> // recipient is not a trader, do logging
                        Logger.log(Messages.getNotATraderMessage(this.peerID, getCurrentTrader().getPeerID()), getPeerLogFile());
            }
        } catch (RemoteException e) {
            Logger.log(e.getMessage(), getPeerLogFile());
            retryTransaction(transactionID, this::initiateBuy, BUY_PERIOD, MAX_ATTEMPTS);
        }
    }
}
