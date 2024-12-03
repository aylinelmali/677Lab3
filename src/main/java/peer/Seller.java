package peer;

import product.Product;
import utils.Logger;
import utils.Messages;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Seller extends APeer {

    public static final int ACCRUAL_PERIOD = 10000; // Tg: time interval for accruing goods (in ms)
    public static final int GOODS_PER_PERIOD = 5; // Ng: number of goods accrued per period

    private Product currentProduct;
    private int inventory; // Seller's local inventory of goods

    public Seller(int peerID) throws NotBoundException, RemoteException {
        super(peerID);
        this.currentProduct = Product.pickRandomProduct();
        this.inventory = 0;
    }

    @Override
    public void start() throws RemoteException {
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        // Periodically accrue goods and attempt to sell them
        executor.scheduleAtFixedRate(() -> {
            // Skip if this seller is a trader
            if (this.peerID == this.traderIDs[0] || this.peerID == this.traderIDs[1]) {
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
        try {
            // Attempt to sell the entire inventory to the current trader
            ReplyStatus status = getCurrentTrader().sell(currentProduct, inventory);

            switch (status) {
                case SUCCESSFUL -> {

                    inventory = 0; // Reset inventory after successful sell
                    // Optionally, pick a new product after selling
                    currentProduct = Product.pickRandomProduct();
                }
                case UNSUCCESSFUL -> {

                }
                case NOT_A_TRADER -> {
                    return;
                }
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }
}
