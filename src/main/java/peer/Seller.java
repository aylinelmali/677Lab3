package peer;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;

public class Seller extends APeer {

    public Seller(int id) throws NotBoundException, RemoteException {
        super(id);
    }

    @Override
    public void start() throws RemoteException {

    }
}
