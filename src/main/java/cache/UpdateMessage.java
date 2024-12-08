package cache;

import product.Product;

import java.io.Serializable;

public record UpdateMessage(int sequenceNumber, int peerID, Product product, int amount) implements Serializable { }
