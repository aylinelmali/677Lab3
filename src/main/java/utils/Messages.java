package utils;

import product.Product;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This class contains all the messages for the logging.
 */

public class Messages {

    public static String getNoInventoryMessage(int buyerID, int traderID, Product product){
        return "Trader " + traderID + " informed buyer " + buyerID + " that no " + product + " was available.";
    }

    public static String getOversoldMessage(){
        return "Warehouse rejected buy request because inventory was oversold.";
    }
}
