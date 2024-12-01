package utils;

import java.io.*;
import java.util.EnumMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import product.Product;
import java.io.BufferedReader;
import java.io.BufferedWriter;

public class Warehouse {
    private static final String INVENTORY_FILE = "warehouse_inventory.txt";
    private final Map<Product, Integer> inventory;

    public Warehouse() throws IOException {
        this.inventory = new EnumMap<>(Product.class);
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

        }
        else {
            inventory.put(product, currentStock - quantity);
            updateInventoryFile();
        }

    }

    public synchronized void sell(Product product, int quantity) throws IOException {
        // Increment the count of itemType in the inventory file.
        inventory.put(product, inventory.getOrDefault(product, 0) + quantity);
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
