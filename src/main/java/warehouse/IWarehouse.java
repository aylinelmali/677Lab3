package warehouse;

import cache.UpdateMessage;
import peer.ReplyStatus;
import product.Product;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IWarehouse extends Remote {

    int lookup(Product product) throws RemoteException;

    ReplyStatus buy(UpdateMessage updateMessage) throws RemoteException;

    ReplyStatus sell(UpdateMessage updateMessage) throws RemoteException;
}
