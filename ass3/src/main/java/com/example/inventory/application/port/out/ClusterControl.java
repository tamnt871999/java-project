package com.example.inventory.application.port.out;

import java.util.List;
import java.util.Map;

public interface ClusterControl {

    ClusterState state();

    ClusterState partition();

    HealReport heal();

    ClusterState reset(Map<String, Integer> seed);

    record ClusterState(String strategy, boolean partitioned, List<NodeState> nodes) {
    }

    record NodeState(String nodeId, String side, Map<String, Integer> available) {
    }

    record HealReport(ClusterState state, List<Reconciliation> reconciliations) {
    }

    record Reconciliation(String sku, int initial, int totalReserved, int mergedAvailable, int oversold) {
    }
}
