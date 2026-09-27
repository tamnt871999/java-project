package com.example.inventory.adapter.out.cluster;

import com.example.inventory.adapter.config.ConnectedNode;
import com.example.inventory.application.port.out.StockRepository;
import com.example.inventory.domain.StockItem;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@ConditionalOnProperty(name = "app.cap.strategy", havingValue = "ap")
class ApStockRepository implements StockRepository {

    private final ClusterNetwork network;
    private final ConnectedNode connectedNode;

    ApStockRepository(ClusterNetwork network, ConnectedNode connectedNode) {
        this.network = network;
        this.connectedNode = connectedNode;
    }

    @Override
    public Optional<StockItem> findBySku(String sku) {
        String nodeId = network.resolve(connectedNode.nodeId());

        return network.availableOn(nodeId, sku)
                .map(available -> StockItem.rehydrate(sku, available));
    }

    @Override
    public void save(StockItem item) {
        String nodeId = network.resolve(connectedNode.nodeId());

        network.writeTo(network.reachableFrom(nodeId), item.sku(), item.available());
    }
}
