package com.example.inventory.cluster;

import com.example.inventory.exception.ClusterUnavailableException;
import com.example.inventory.exception.SkuNotFoundException;

import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class CpStrategy implements CapStrategy {

    private final ClusterNodes nodes;

    public CpStrategy(ClusterNodes nodes) {
        this.nodes = nodes;
    }

    @Override
    public String name() {
        return "cp";
    }

    @Override
    public String summary() {
        return "Uu tien Consistency: node khong lien lac du quorum thi tu choi ca doc lan ghi";
    }

    @Override
    public int read(String nodeId, String sku) {
        requireQuorum(nodeId);
        return nodes.availableOn(nodeId, sku).orElseThrow(() -> new SkuNotFoundException(sku));
    }

    @Override
    public void write(String nodeId, String sku, int available) {
        Set<String> reachable = requireQuorum(nodeId);
        nodes.writeTo(reachable, sku, available);
    }

    @Override
    public int mergeOnHeal(String sku, int initial) {
        return nodes.availableOn(nodes.majorityNode(), sku).orElse(initial);
    }

    private Set<String> requireQuorum(String nodeId) {
        Set<String> reachable = nodes.reachableFrom(nodeId);
        if (reachable.size() < nodes.quorum()) {
            throw new ClusterUnavailableException(
                    "Node " + nodeId + " chi lien lac duoc " + reachable.size()
                            + " node, can toi thieu " + nodes.quorum());
        }
        return reachable;
    }
}
