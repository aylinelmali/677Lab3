package utils;

import java.io.*;
import java.util.EnumMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import product.Product;
import utils.Logger;
import utils.Messages;
import java.io.BufferedReader;
import java.io.BufferedWriter;

public class Warehouse {
    private static final String INVENTORY_FILE = "warehouse_inventory.txt";
    private static final String LOG_FILE = "warehouse_log.txt";
    private final Map<Product, Integer> inventory;
    private final Logger logger;

    public Warehouse() throws IOException {
        this.inventory = new EnumMap<>(Product.class);
        this.logger = new Logger(LOG_FILE);
        loadInventory();
    }

    private void loadInventory() throws IOException{
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(INVENTORY_FILE))) {
            for (Product product : Product.values()) {
                inventory.put(product, 0); //Start with zero stock
                writer.write(product + ",0");
                writer.newLine();
            }
        }
    }

    public synchronized int lookup(Product product) {
        // Read inventory file and return the count of itemType.
        return inventory.getOrDefault(product, 0);
    }

    public synchronized void buy(Product product, int quantity) throws IOException {
        // Decrement the count of itemType in the inventory file.
        int currentStock = inventory.getOrDefault(product, 0);
        if (currentStock < quantity) {
            logger.log(Messages.getOversoldMessage());
        }
        else {
            inventory.put(product, currentStock - quantity);
            logger.log(Messages.getWarehouseBuyMessage(product, quantity));
            updateInventoryFile();
        }

    }

    public synchronized void sell(Product product, int quantity) throws IOException {
        // Increment the count of itemType in the inventory file.
        inventory.put(product, inventory.getOrDefault(product, 0) + quantity);
        logger.log(Messages.getWarehouseSellMessage(product, quantity));
        updateInventoryFile();
    }

    // Update the inventory file to reflect the current state
    private void updateInventoryFile() throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(INVENTORY_FILE))) {
            for (Map.Entry<Product, Integer> entry : inventory.entrySet()) {
                writer.write(entry.getKey() + "," + entry.getValue());
                writer.newLine();
            }
        }
    }
}
