package com.example.inventory.application.port.out;

import com.example.inventory.domain.StockItem;

import java.util.Optional;

public interface StockRepository {

    Optional<StockItem> findBySku(String sku);

    void save(StockItem item);
}
