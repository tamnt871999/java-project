package com.example.inventory.cluster;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
public class ClusterNodes {

    public static final String DEFAULT_NODE = "node-1";
    public static final Map<String, Integer> DEFAULT_SEED = Map.of("TSHIRT-M", 5);

    private static final List<String> MAJORITY_SIDE = List.of("node-1", "node-2");
    private static final List<String> MINORITY_SIDE = List.of("node-3");

    private final Map<String, Map<String, Integer>> availableByNode = new LinkedHashMap<>();
    private final Map<String, Integer> initialBySku = new LinkedHashMap<>();
    private boolean partitioned;

    public ClusterNodes() {
        reset(DEFAULT_SEED);
    }

    public synchronized void reset(Map<String, Integer> seed) {
        Map<String, Integer> effective = seed == null || seed.isEmpty() ? DEFAULT_SEED : seed;

        partitioned = false;
        initialBySku.clear();
        initialBySku.putAll(effective);

        availableByNode.clear();
        for (String nodeId : allNodeIds()) {
            availableByNode.put(nodeId, new LinkedHashMap<>(effective));
        }
    }

    public synchronized String requireKnownNode(String nodeId) {
        if (nodeId == null || nodeId.isBlank()) {
            return DEFAULT_NODE;
        }
        if (!availableByNode.containsKey(nodeId)) {
            throw new IllegalArgumentException(
                    "Khong co node '" + nodeId + "'. Cac node hop le: " + availableByNode.keySet());
        }
        return nodeId;
    }

    public synchronized boolean isPartitioned() {
        return partitioned;
    }

    public synchronized void setPartitioned(boolean value) {
        partitioned = value;
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

    public synchronized void writeTo(Collection<String> nodeIds, String sku, int available) {
        nodeIds.forEach(nodeId -> availableByNode.get(nodeId).put(sku, available));
    }

    public synchronized Map<String, Integer> initialBySku() {
        return Map.copyOf(initialBySku);
    }

    public synchronized Map<String, Integer> availableOn(String nodeId) {
        return Map.copyOf(availableByNode.get(nodeId));
    }

    public synchronized List<String> allNodeIds() {
        if (availableByNode.isEmpty()) {
            List<String> ids = new java.util.ArrayList<>(MAJORITY_SIDE);
            ids.addAll(MINORITY_SIDE);
            return ids;
        }
        return List.copyOf(availableByNode.keySet());
    }

    public synchronized String sideOf(String nodeId) {
        if (!partitioned) {
            return "connected";
        }
        return MAJORITY_SIDE.contains(nodeId) ? "majority" : "minority";
    }

    public synchronized String majorityNode() {
        return MAJORITY_SIDE.get(0);
    }

    public synchronized String minorityNode() {
        return MINORITY_SIDE.get(0);
    }
}
