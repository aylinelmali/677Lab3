package warehouse;

import java.io.*;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.EnumMap;
import java.util.Map;

import peer.ReplyStatus;
import product.Product;
import utils.Logger;
import utils.Messages;

import java.io.BufferedWriter;

public class Warehouse extends UnicastRemoteObject implements Remote {

    public static final int REGISTRY_PORT = 1099;
    private static final String INVENTORY_FILE = "warehouse_inventory.txt";
    public static final String WAREHOUSE_LOG_FILE = "warehouse_log.txt";
    public static final String WAREHOUSE_NAME = "warehouse";

    public static void main(String[] args) throws RemoteException {
        Registry registry = LocateRegistry.getRegistry("127.0.0.1", REGISTRY_PORT);
        registry.rebind(WAREHOUSE_NAME, new Warehouse());
    }

    // CLASS

    private final Map<Product, Integer> inventory;

    private Warehouse() throws RemoteException {
        super();
        this.inventory = new EnumMap<>(Product.class);
        loadInventory();
    }

    private void loadInventory() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(INVENTORY_FILE))) {
            for (Product product : Product.values()) {
                inventory.put(product, 0); // start with zero stock
                writer.write(product + ",0");
                writer.newLine();
            }
        } catch (IOException e) {
            // TODO: Do proper logging.
        }
    }

    public synchronized int lookup(Product product) throws RemoteException {
        // read inventory file and return the count of itemType.
        return inventory.getOrDefault(product, 0);
    }

    public synchronized ReplyStatus buy(Product product, int quantity) throws RemoteException {
        // decrement the count of itemType in the inventory file.
        int currentStock = inventory.getOrDefault(product, 0);
        if (currentStock < quantity) {
            Logger.log(Messages.getOversoldMessage(), WAREHOUSE_LOG_FILE);
            return ReplyStatus.UNSUCCESSFUL;
        }
        inventory.put(product, currentStock - quantity);
        Logger.log(Messages.getWarehouseBuyMessage(product, quantity), WAREHOUSE_LOG_FILE);
        try {
            updateInventoryFile();
        } catch (IOException e) {
            return ReplyStatus.UNSUCCESSFUL;
        }
        return ReplyStatus.SUCCESSFUL;
    }

    public synchronized ReplyStatus sell(Product product, int quantity) throws RemoteException {
        // increment the count of itemType in the inventory file.
        inventory.put(product, inventory.getOrDefault(product, 0) + quantity);
        Logger.log(Messages.getWarehouseSellMessage(product, quantity), WAREHOUSE_LOG_FILE);
        try {
            updateInventoryFile();
        } catch (IOException e) {
            return ReplyStatus.UNSUCCESSFUL;
        }
        return ReplyStatus.SUCCESSFUL;
    }

    // update the inventory file to reflect the current state
    private void updateInventoryFile() throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(INVENTORY_FILE))) {
            for (Map.Entry<Product, Integer> entry : inventory.entrySet()) {
                writer.write(entry.getKey() + "," + entry.getValue());
                writer.newLine();
            }
        }
    }
}
