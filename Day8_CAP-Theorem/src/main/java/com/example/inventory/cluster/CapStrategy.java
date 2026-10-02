package com.example.inventory.cluster;

public interface CapStrategy {

    String name();

    String summary();

    int read(String nodeId, String sku);

    void write(String nodeId, String sku, int available);

    int mergeOnHeal(String sku, int initial);
}
