package warehouse;

import cache.UpdateMessage;
import peer.ReplyStatus;
import product.Product;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IWarehouse extends Remote {

    /**
     * Used for looking up the amount of product.
     * @param product Product to lookup.
     * @return Returns the amount of product in the inventory.
     */
    int lookup(Product product) throws RemoteException;

    /**
     * Buys a specific amount of products and removes them from the inventory.
     * @param updateMessage Contains important information, such as product type and amount.
     * @return Returns the reply status.
     */
    ReplyStatus buy(UpdateMessage updateMessage) throws RemoteException;

    /**
     * Sells a specific amount of items to the warehouse.
     * @param updateMessage Contains important information, such as product type and amount.
     * @return Returns the reply status.
     */
    ReplyStatus sell(UpdateMessage updateMessage) throws RemoteException;
}
