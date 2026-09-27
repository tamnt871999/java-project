package com.example.inventory.application.usecase;

import com.example.inventory.application.port.in.GetStockUseCase;
import com.example.inventory.application.port.in.SkuNotFoundException;
import com.example.inventory.application.port.out.StockRepository;
import com.example.inventory.domain.StockItem;

import org.springframework.stereotype.Service;

@Service
class GetStockService implements GetStockUseCase {

    private final StockRepository stockRepository;

    GetStockService(StockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    @Override
    public StockView getStock(String sku) {
        StockItem item = stockRepository.findBySku(sku)
                .orElseThrow(() -> new SkuNotFoundException(sku));

        return new StockView(item.sku(), item.available());
    }
}
