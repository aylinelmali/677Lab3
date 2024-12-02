package peer;

import product.Product;
import utils.Logger;
import utils.Messages;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Buyer extends APeer{

    public static final int BUY_PERIOD = 5000;
    public static final int MAX_ATTEMPTS = 3;

    public int retries;
    public Product product;
    public int amount;

    public Buyer(int peerID) throws RemoteException, NotBoundException {
        super(peerID);
        retries = 0;
    }

    @Override
    public void start() throws RemoteException {
        Logger.log("Peer " + peerID + " (Buyer)", getPeerLogFile());
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

        executor.scheduleAtFixedRate(() -> {
            // only buy something if not coordinator
            if (this.peerID == this.traderIDs[0] || this.peerID == this.traderIDs[1]) {
                return;
            }

            // buy new product when retries equals 0
            if (this.retries == 0 || this.retries >= MAX_ATTEMPTS) {
                this.product = Product.pickRandomProduct();
                this.amount = (int) (Math.random() * 3) + 1;
            }

            initiateBuy();

        }, BUY_PERIOD, BUY_PERIOD, TimeUnit.MILLISECONDS);
    }

    public void initiateBuy() {
        try {
            // buy try to buy product
            ReplyStatus status = getCurrentTrader().buy(this.product, this.amount);

            switch (status) {
                case SUCCESSFUL -> {
                    // reset retries to pick a new product
                    this.retries = 0;
                    Logger.log(Messages.getBuySuccessfulMessage(this.peerID, this.product, this.amount), getPeerLogFile());
                }
                case UNSUCCESSFUL -> {
                    // buy unsuccessful, increment reset counter
                    this.retries++;
                    Logger.log(Messages.getBuyUnsuccessfulMessage(this.peerID, this.product, this.amount, this.retries < MAX_ATTEMPTS), getPeerLogFile());
                }
                case NOT_A_TRADER -> {
                    // recipient is not a trader, do logging
                    Logger.log(Messages.getNotATraderMessage(this.peerID, getCurrentTrader().getPeerID()), getPeerLogFile());
                    return;
                }
            }
        } catch (RemoteException e) {
            // TODO: Proper logging
            throw new RuntimeException(e);
        }
    }
}
