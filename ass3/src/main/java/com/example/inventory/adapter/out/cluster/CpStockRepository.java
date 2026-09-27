package com.example.inventory.adapter.out.cluster;

import com.example.inventory.adapter.config.ConnectedNode;
import com.example.inventory.application.port.out.ClusterUnavailableException;
import com.example.inventory.application.port.out.StockRepository;
import com.example.inventory.domain.StockItem;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;

@Component
@ConditionalOnProperty(name = "app.cap.strategy", havingValue = "cp", matchIfMissing = true)
class CpStockRepository implements StockRepository {

    private final ClusterNetwork network;
    private final ConnectedNode connectedNode;

    CpStockRepository(ClusterNetwork network, ConnectedNode connectedNode) {
        this.network = network;
        this.connectedNode = connectedNode;
    }

    @Override
    public Optional<StockItem> findBySku(String sku) {
        String nodeId = network.resolve(connectedNode.nodeId());
        requireQuorum(nodeId);

        return network.availableOn(nodeId, sku)
                .map(available -> StockItem.rehydrate(sku, available));
    }

    @Override
    public void save(StockItem item) {
        String nodeId = network.resolve(connectedNode.nodeId());
        Set<String> reachable = requireQuorum(nodeId);

        network.writeTo(reachable, item.sku(), item.available());
    }

    private Set<String> requireQuorum(String nodeId) {
        Set<String> reachable = network.reachableFrom(nodeId);
        if (reachable.size() < network.quorum()) {
            throw new ClusterUnavailableException(
                    "Node " + nodeId + " chi lien lac duoc " + reachable.size()
                            + " node, can toi thieu " + network.quorum());
        }
        return reachable;
    }
}
