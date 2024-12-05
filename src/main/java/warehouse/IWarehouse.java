package warehouse;

import peer.ReplyStatus;
import product.Product;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IWarehouse extends Remote {

    int lookup(Product product) throws RemoteException;

    ReplyStatus buy(Product product, int quantity) throws RemoteException;

    ReplyStatus sell(Product product, int quantity) throws RemoteException;
}
