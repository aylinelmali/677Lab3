package cache;

import product.Product;

import java.io.Serializable;

public record CacheUpdateMessage(int sequenceNumber, int peerID, Product product, int stock) implements Serializable { }
