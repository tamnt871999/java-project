package com.example.inventory.service;

import com.example.inventory.cluster.CapStrategies;
import com.example.inventory.cluster.CapStrategy;
import com.example.inventory.cluster.ClusterNodes;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ClusterService {

    private final ClusterNodes nodes;
    private final CapStrategies strategies;

    public ClusterService(ClusterNodes nodes, CapStrategies strategies) {
        this.nodes = nodes;
        this.strategies = strategies;
    }

    public ClusterState state() {
        List<NodeState> nodeStates = nodes.allNodeIds().stream()
                .map(nodeId -> new NodeState(nodeId, nodes.sideOf(nodeId), nodes.availableOn(nodeId)))
                .toList();

        return new ClusterState(nodes.isPartitioned(), nodes.quorum(), nodeStates);
    }

    public ClusterState reset(Map<String, Integer> seed) {
        nodes.reset(seed);
        return state();
    }

    public ClusterState partition() {
        nodes.setPartitioned(true);
        return state();
    }

    public HealReport heal(String strategyName) {
        CapStrategy strategy = strategies.resolve(strategyName);
        List<Reconciliation> reconciliations = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : nodes.initialBySku().entrySet()) {
            String sku = entry.getKey();
            int initial = entry.getValue();
            int merged = strategy.mergeOnHeal(sku, initial);

            reconciliations.add(new Reconciliation(
                    sku, initial, initial - merged, merged, Math.max(0, -merged)));

            nodes.writeTo(nodes.allNodeIds(), sku, Math.max(0, merged));
        }

        nodes.setPartitioned(false);
        return new HealReport(strategy.name(), state(), reconciliations);
    }

    public record ClusterState(boolean partitioned, int quorum, List<NodeState> nodes) {
    }

    public record NodeState(String nodeId, String side, Map<String, Integer> available) {
    }

    public record HealReport(String strategy, ClusterState state,
                             List<Reconciliation> reconciliations) {
    }

    public record Reconciliation(String sku, int initial, int totalReserved,
                                 int mergedAvailable, int oversold) {
    }
}
