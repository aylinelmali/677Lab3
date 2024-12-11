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

    public static String getOversoldMessage(){
        return "Warehouse rejected buy request because inventory was oversold.";
    }

    public static String getWarehouseSellMessage(Product product, int quantity) {
        return "Sell request processed by warehouse: Added " + quantity + " of " + product + " to the inventory.";
    }

    public static String getWarehouseBuyMessage(Product product, int quantity) {
        return "Buy request processed by warehouse: Removed " + quantity + " of " + product + " from the inventory.";
    }

    public static String getBuyAttemptMessage(int buyerID, int traderID, Product product, int quantity) {
        return "Peer " + buyerID + " attempts buying " + quantity + " piece(s) of " + product + " from trader " + traderID + ".";
    }

    public static String getBuySuccessfulMessage(int buyerID, int traderID, Product product, int quantity) {
        return "Peer " + buyerID + " successful bought " + quantity + " piece(s) of " + product + " from trader " + traderID + ".";
    }

    public static String getOutOfStockMessage(int buyerID, int traderID, Product product, int quantity) {
        return "Peer " + buyerID + " couldn't buy " + quantity + " piece(s) of " + product + " from trader " + traderID + ". Item out of stock.";
    }

    public static String getBuyLowSequenceNumberMessage(int buyerID, int traderID, Product product, int quantity) {
        return "Peer " + buyerID + " couldn't buy " + quantity + " piece(s) of " + product + " from trader " + traderID + ". Sequence number too low .";
    }

    public static String getBuyErrorMessage(int buyerID, int traderID, Product product, int quantity) {
        return "Peer " + buyerID + " couldn't buy " + quantity + " piece(s) of " + product + " from trader " + traderID + ". Error during write.";
    }

    public static String getSellAttemptMessage(int sellerID, int traderID, Product product, int quantity) {
        return "Peer " + sellerID + " attempts selling " + quantity + " piece(s) of " + product + " to trader " + traderID + ".";
    }

    public static String getSellSuccessfulMessage(int sellerID, int traderID, Product product, int quantity) {
        return "Peer " + sellerID + " successful sold " + quantity + " piece(s) of " + product + " to trader " + traderID + ".";
    }

    public static String getSellLowSequenceNumberMessage(int sellerID, int traderID, Product product, int quantity) {
        return "Peer " + sellerID + " couldn't sell " + quantity + " piece(s) of " + product + " to trader " + traderID + ". Sequence number too low.";
    }

    public static String getSellErrorMessage(int buyerID, int traderID, Product product, int quantity) {
        return "Peer " + buyerID + " couldn't sell " + quantity + " piece(s) of " + product + " from trader " + traderID + ". Error during write.";
    }

    public static String getNotATraderMessage(int peerID, int traderID) {
        return "Peer " + peerID + " contacted peer " + traderID + ". Peer " + traderID + " is not a trader.";
    }

    public static String getSendHeartbeatMessage(int senderID, int receiverID) {
        return "Peer " + senderID + " sends heartbeat message to " + receiverID + ".";
    }

    public static String getHeartbeatResponseMessage(int senderID, int receiverID) {
        return "Peer " + receiverID + " responded to heartbeat message from peer " + senderID + ".";
    }

    public static String getHeartbeatTimeoutMessage(int senderID, int receiverID) {
        return "Peer " + senderID + " didn't receive a heartbeat response from peer " + receiverID + ". Notifying all peers.";
    }

    public static String getTraderUpdatedMessage(int peerID, int newTraderID) {
        return "Peer " + peerID + " updated trader to peer " + newTraderID + ".";
    }
}
