package warehouse;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Warehouse extends UnicastRemoteObject implements IWarehouse {

    public static final int REGISTRY_PORT = 1099;
    private static final String INVENTORY_FILE = "warehouse_inventory.txt";
    public static final String WAREHOUSE_LOG_FILE = "warehouse_log.txt";
    public static final String WAREHOUSE_NAME = "warehouse";

    public static final String STATS_FILE = "stats3.txt";

    public static void main(String[] args) throws RemoteException {
        Registry registry = LocateRegistry.getRegistry("127.0.0.1", REGISTRY_PORT);
        registry.rebind(WAREHOUSE_NAME, new Warehouse());
    }

    // CLASS
    private final Map<Integer, Integer> peerIDtoSequenceNumber; // assures that there are no duplicate messages per peer.

    public Warehouse() throws RemoteException {
        super();
        resetInventory();
        this.peerIDtoSequenceNumber = new HashMap<>();
    }

    private void resetInventory() {
        try {
            writeInventoryFile(new HashMap<>());
        } catch (IOException ignore) {}
    }

    @Override
    public synchronized int lookup(Product product) throws RemoteException {
        // read inventory file and return the count of itemType.
        try {
            Map<Product, Integer> inventory = readInventoryFile();
            return inventory.getOrDefault(product, 0);
        } catch (IOException ignore) {}
        return 0;
    }

    @Override
    public synchronized ReplyStatus buy(UpdateMessage updateMessage) throws RemoteException {

        // check if sequence number is valid
        if (updateMessage.sequenceNumber() <= peerIDtoSequenceNumber.getOrDefault(updateMessage.peerID(), 0)) {
            return ReplyStatus.LOW_SEQUENCE_NUMBER;
        }

        // decrement the count of itemType in the inventory file.
        try {
            Map<Product, Integer> inventory = readInventoryFile();
            int currentStock = inventory.getOrDefault(updateMessage.product(), 0);
            // check if item is in stock
            if (currentStock < updateMessage.amount()) {
                Logger.log(Messages.getOversoldMessage(), WAREHOUSE_LOG_FILE);
                return ReplyStatus.NOT_IN_STOCK;
            }
            // update inventory and save to file
            inventory.put(updateMessage.product(), currentStock - updateMessage.amount());
            Logger.log(Messages.getWarehouseBuyMessage(updateMessage.product(), updateMessage.amount()), WAREHOUSE_LOG_FILE);
            writeInventoryFile(inventory);
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

        // increment the count of itemType in the inventory and write to file.
        try {
            Map<Product, Integer> inventory = readInventoryFile();
            inventory.put(updateMessage.product(), inventory.getOrDefault(updateMessage.product(), 0) + updateMessage.amount());
            Logger.log(Messages.getWarehouseSellMessage(updateMessage.product(), updateMessage.amount()), WAREHOUSE_LOG_FILE);
            writeInventoryFile(inventory);
        } catch (IOException e) {
            return ReplyStatus.ERROR_DURING_WRITE;
        }

        // update sequence number
        peerIDtoSequenceNumber.put(updateMessage.peerID(), updateMessage.sequenceNumber());
        return ReplyStatus.SUCCESSFUL;
    }

    // FILE MANAGEMENT

    /**
     * Takes the values of the inventory map and writes it into the warehouse file.
     * @param inventory The inventory of the warehouse.
     */
    private void writeInventoryFile(Map<Product, Integer> inventory) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (var entry : inventory.entrySet()) {
            sb
                    .append(entry.getKey())
                    .append(",")
                    .append(entry.getValue())
                    .append("\n");
        }
        createFile(sb.toString());
    }

    /**
     * Reads the contents of the warehouse file and creates a HashMap.
     * @return A map containing products and their respective amounts.
     */
    private Map<Product, Integer> readInventoryFile() throws IOException {
        Map<Product, Integer> inventory = new HashMap<>();

        String content = readFile();
        for (String line : content.split("\n")) {
            if (line.isBlank()) {
                continue;
            }
            String[] parts = line.split(",");
            Product product = Product.valueOf(parts[0].toUpperCase());
            int amount = Integer.parseInt(parts[1]);
            inventory.put(product, amount);
        }

        return inventory;
    }

    /**
     * Writes a string to text file to save trader's current state.
     * @param content Content to write to file.
     */
    private void createFile(String content) throws IOException {
        BufferedWriter writer = Files.newBufferedWriter(Paths.get(INVENTORY_FILE), StandardCharsets.UTF_8);
        writer.write(content);
        writer.close();
    }

    /**
     * Reads the contexts of text file into string.
     * @return Content of the file.
     */
    private String readFile() throws IOException {
        StringBuilder text = new StringBuilder();
        BufferedReader reader = Files.newBufferedReader(Paths.get(INVENTORY_FILE), StandardCharsets.UTF_8);
        for (String line = reader.readLine(); line != null; line = reader.readLine())
            text.append(line).append("\n");

        reader.close();

        return text.toString();
    }
}
