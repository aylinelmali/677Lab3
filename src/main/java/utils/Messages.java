package utils;

import product.Product;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This class contains all the messages for the logging.
 */

public class Messages {

    public static String getElectionDoneMessage(int[] traderIDs) {
        return "Election done. New coordinators are peers " +  Arrays.toString(traderIDs) + ".";
    }

    public static String getPeerDoingElectionMessage(int peerID, int[] newTags) {
        return "Peer " + peerID + " is doing election. Tags: " + Arrays.toString(newTags) + ".";
    }

    public static String getPeerDoesNotRespondMessage(int peerID) {
        return "Peer " + peerID + " doesn't respond.";
    }

    public static String getPeerUpdatesCoordinatorMessage(int peerID, int[] traderIDs) {
        return "Peer " + peerID + " sets coordinators to " + Arrays.toString(traderIDs) + ".";
    }

    public static String getNoInventoryMessage(int buyerID, int traderID, Product product) {
        return "Trader " + traderID + " informed buyer " + buyerID + " that no " + product + " was available.";
    }

    public static String getOversoldMessage(){
        return "Warehouse rejected buy request because inventory was oversold.";
    }

    public static String getWarehouseSellMessage(Product product, int quantity) {
        return "Sell request processed by warehouse: Added " + quantity + " of " + product + " to the inventory.";
    }

    public static String getWarehouseBuyMessage(Product product, int quantity) {
        return "Buy request processed by warehouse: Removed " + quantity + " of " + product + " from the inventory.";
    }

    public static String getBuySuccessfulMessage(int peerID, int traderID, Product product, int quantity) {
        return "Peer " + peerID + " successful bought " + quantity + " piece(s) of " + product + " from trader " + traderID + ".";
    }

    public static String getBuyUnsuccessfulMessage(int peerID, int traderID, Product product, int quantity, boolean retry) {
        String msg = "Peer " + peerID + " couldn't buy " + quantity + " piece(s) of " + product + " from trader " + traderID + ".";
        msg += retry ? "Retrying." : "Picking new product.";
        return msg;
    }

    public static String getNotATraderMessage(int peerID, int traderID) {
        return "Peer " + peerID + " contacted peer " + traderID + ". Peer " + traderID + " is not a trader.";
    }
}
