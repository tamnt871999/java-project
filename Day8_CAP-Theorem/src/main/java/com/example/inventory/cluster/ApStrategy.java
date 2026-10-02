package com.example.inventory.cluster;

import com.example.inventory.exception.SkuNotFoundException;

import org.springframework.stereotype.Component;

@Component
public class ApStrategy implements CapStrategy {

    private final ClusterNodes nodes;

    public ApStrategy(ClusterNodes nodes) {
        this.nodes = nodes;
    }

    @Override
    public String name() {
        return "ap";
    }

    @Override
    public String summary() {
        return "Uu tien Availability: node nao cung phuc vu bang ban sao tai cho, hoa giai sau";
    }

    @Override
    public int read(String nodeId, String sku) {
        return nodes.availableOn(nodeId, sku).orElseThrow(() -> new SkuNotFoundException(sku));
    }

    @Override
    public void write(String nodeId, String sku, int available) {
        nodes.writeTo(nodes.reachableFrom(nodeId), sku, available);
    }

    @Override
    public int mergeOnHeal(String sku, int initial) {
        int majority = nodes.availableOn(nodes.majorityNode(), sku).orElse(initial);
        if (!nodes.isPartitioned()) {
            return majority;
        }
        int minority = nodes.availableOn(nodes.minorityNode(), sku).orElse(initial);
        return initial - (initial - majority) - (initial - minority);
    }
}
