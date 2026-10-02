package com.example.inventory.service;

import com.example.inventory.cluster.CapStrategies;
import com.example.inventory.cluster.CapStrategy;
import com.example.inventory.cluster.ClusterNodes;
import com.example.inventory.exception.BusinessRuleException;

import org.springframework.stereotype.Service;

@Service
public class StockService {

    private final ClusterNodes nodes;
    private final CapStrategies strategies;

    public StockService(ClusterNodes nodes, CapStrategies strategies) {
        this.nodes = nodes;
        this.strategies = strategies;
    }

    public Reservation reserve(String strategyName, String nodeId, String sku, int quantity) {
        CapStrategy strategy = strategies.resolve(strategyName);
        String node = nodes.requireKnownNode(nodeId);

        if (quantity <= 0) {
            throw new BusinessRuleException("So luong giu cho phai lon hon 0");
        }

        int available = strategy.read(node, sku);
        if (quantity > available) {
            throw new BusinessRuleException(
                    "Ton kho khong du: con " + available + ", can " + quantity);
        }

        int remaining = available - quantity;
        strategy.write(node, sku, remaining);

        return new Reservation(sku, quantity, remaining, node, strategy.name());
    }

    public StockView getStock(String strategyName, String nodeId, String sku) {
        CapStrategy strategy = strategies.resolve(strategyName);
        String node = nodes.requireKnownNode(nodeId);

        return new StockView(sku, strategy.read(node, sku), node, strategy.name());
    }

    public record Reservation(String sku, int reserved, int available,
                              String servedBy, String strategy) {
    }

    public record StockView(String sku, int available, String servedBy, String strategy) {
    }
}
