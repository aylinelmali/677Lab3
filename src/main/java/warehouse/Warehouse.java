package warehouse;

import java.io.*;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import cache.UpdateMessage;
import peer.ReplyStatus;
import product.Product;
import utils.Logger;
import utils.Messages;

import java.io.BufferedWriter;

public class Warehouse extends UnicastRemoteObject implements IWarehouse {

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
    private final Map<Integer, Integer> peerIDtoSequenceNumber; // assures that there are no duplicate messages per peer.

    public Warehouse() throws RemoteException {
        super();
        this.inventory = new EnumMap<>(Product.class);
        loadInventory();
        this.peerIDtoSequenceNumber = new HashMap<>();
    }

    private void loadInventory() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(INVENTORY_FILE))) {
            for (Product product : Product.values()) {
                inventory.put(product, 0); // start with zero stock
                writer.write(product + ",0");
                writer.newLine();
            }
        } catch (IOException ignored) {}
    }

    @Override
    public synchronized int lookup(Product product) throws RemoteException {
        // read inventory file and return the count of itemType.
        return inventory.getOrDefault(product, 0);
    }

    @Override
    public synchronized ReplyStatus buy(UpdateMessage updateMessage) throws RemoteException {

        // check if sequence number is valid
        if (updateMessage.sequenceNumber() <= peerIDtoSequenceNumber.getOrDefault(updateMessage.peerID(), 0)) {
            System.out.println(peerIDtoSequenceNumber);
            System.out.println("Peer " + updateMessage.peerID() + " has sequence number " + updateMessage.sequenceNumber());
            return ReplyStatus.LOW_SEQUENCE_NUMBER;
        }

        // decrement the count of itemType in the inventory file.
        int currentStock = inventory.getOrDefault(updateMessage.product(), 0);
        if (currentStock < updateMessage.amount()) {
            Logger.log(Messages.getOversoldMessage(), WAREHOUSE_LOG_FILE);
            return ReplyStatus.NOT_IN_STOCK;
        }
        inventory.put(updateMessage.product(), currentStock - updateMessage.amount());
        Logger.log(Messages.getWarehouseBuyMessage(updateMessage.product(), updateMessage.amount()), WAREHOUSE_LOG_FILE);
        try {
            updateInventoryFile();
        } catch (IOException e) {
            return ReplyStatus.ERROR_DURING_WRITE;
        }

        // update sequence number
        peerIDtoSequenceNumber.put(updateMessage.peerID(), updateMessage.sequenceNumber());
        return ReplyStatus.SUCCESSFUL;
    }

    @Override
    public synchronized ReplyStatus sell(UpdateMessage updateMessage) throws RemoteException {

        // check if sequence number is valid
        if (updateMessage.sequenceNumber() <= peerIDtoSequenceNumber.getOrDefault(updateMessage.peerID(), 0)) {
            return ReplyStatus.LOW_SEQUENCE_NUMBER;
        }

        // increment the count of itemType in the inventory file.
        inventory.put(updateMessage.product(), inventory.getOrDefault(updateMessage.product(), 0) + updateMessage.amount());
        Logger.log(Messages.getWarehouseSellMessage(updateMessage.product(), updateMessage.amount()), WAREHOUSE_LOG_FILE);
        try {
            updateInventoryFile();
        } catch (IOException e) {
            return ReplyStatus.ERROR_DURING_WRITE;
        }

        // update sequence number
        peerIDtoSequenceNumber.put(updateMessage.peerID(), updateMessage.sequenceNumber());
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
