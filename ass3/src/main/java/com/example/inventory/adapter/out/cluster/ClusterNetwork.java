package com.example.inventory.adapter.out.cluster;

import com.example.inventory.application.port.out.ClusterControl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
public class ClusterNetwork implements ClusterControl {

    public static final String LEADER = "node-1";

    private static final List<String> MAJORITY_SIDE = List.of("node-1", "node-2");
    private static final List<String> MINORITY_SIDE = List.of("node-3");
    private static final Map<String, Integer> DEFAULT_SEED = Map.of("TSHIRT-M", 5);

    private final String strategy;
    private final Map<String, Map<String, Integer>> availableByNode = new LinkedHashMap<>();
    private final Map<String, Integer> initialBySku = new LinkedHashMap<>();
    private boolean partitioned;

    ClusterNetwork(@Value("${app.cap.strategy}") String strategy) {
        this.strategy = strategy;
        reset(DEFAULT_SEED);
    }

    public synchronized String resolve(String requestedNodeId) {
        if (requestedNodeId == null || requestedNodeId.isBlank()) {
            return LEADER;
        }
        if (!availableByNode.containsKey(requestedNodeId)) {
            throw new IllegalArgumentException(
                    "Khong co node '" + requestedNodeId + "'. Cac node hop le: "
                            + availableByNode.keySet());
        }
        return requestedNodeId;
    }

    public synchronized Set<String> reachableFrom(String nodeId) {
        if (!partitioned) {
            return new LinkedHashSet<>(availableByNode.keySet());
        }
        return new LinkedHashSet<>(MAJORITY_SIDE.contains(nodeId) ? MAJORITY_SIDE : MINORITY_SIDE);
    }

    public synchronized int quorum() {
        return availableByNode.size() / 2 + 1;
    }

    public synchronized Optional<Integer> availableOn(String nodeId, String sku) {
        return Optional.ofNullable(availableByNode.get(nodeId).get(sku));
    }

    public synchronized void writeTo(Set<String> nodeIds, String sku, int available) {
        nodeIds.forEach(nodeId -> availableByNode.get(nodeId).put(sku, available));
    }

    @Override
    public synchronized ClusterState state() {
        List<NodeState> nodes = availableByNode.entrySet().stream()
                .map(entry -> new NodeState(entry.getKey(), sideOf(entry.getKey()),
                        Map.copyOf(entry.getValue())))
                .toList();
        return new ClusterState(strategy, partitioned, nodes);
    }

    @Override
    public synchronized ClusterState partition() {
        partitioned = true;
        return state();
    }

    @Override
    public synchronized HealReport heal() {
        List<Reconciliation> reconciliations = new ArrayList<>();

        for (Map.Entry<String, Integer> seed : initialBySku.entrySet()) {
            String sku = seed.getKey();
            int initial = seed.getValue();
            int merged = mergeSku(sku, initial);
            int oversold = Math.max(0, -merged);

            reconciliations.add(new Reconciliation(
                    sku, initial, initial - merged, merged, oversold));

            writeTo(new LinkedHashSet<>(availableByNode.keySet()), sku, Math.max(0, merged));
        }

        partitioned = false;
        return new HealReport(state(), reconciliations);
    }

    @Override
    public synchronized ClusterState reset(Map<String, Integer> seed) {
        Map<String, Integer> effective = seed == null || seed.isEmpty() ? DEFAULT_SEED : seed;

        partitioned = false;
        initialBySku.clear();
        initialBySku.putAll(effective);

        availableByNode.clear();
        for (String nodeId : allNodeIds()) {
            availableByNode.put(nodeId, new LinkedHashMap<>(effective));
        }
        return state();
    }

    private int mergeSku(String sku, int initial) {
        int majorityAvailable = availableByNode.get(MAJORITY_SIDE.get(0)).getOrDefault(sku, initial);
        if (!partitioned) {
            return majorityAvailable;
        }
        if ("cp".equalsIgnoreCase(strategy)) {
            return majorityAvailable;
        }
        int minorityAvailable = availableByNode.get(MINORITY_SIDE.get(0)).getOrDefault(sku, initial);
        return initial - (initial - majorityAvailable) - (initial - minorityAvailable);
    }

    private String sideOf(String nodeId) {
        if (!partitioned) {
            return "connected";
        }
        return MAJORITY_SIDE.contains(nodeId) ? "majority" : "minority";
    }

    private static List<String> allNodeIds() {
        List<String> nodeIds = new ArrayList<>(MAJORITY_SIDE);
        nodeIds.addAll(MINORITY_SIDE);
        return nodeIds;
    }
}
