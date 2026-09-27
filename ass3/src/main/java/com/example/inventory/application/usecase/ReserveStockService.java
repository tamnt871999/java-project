package com.example.inventory.application.usecase;

import com.example.inventory.application.port.in.ReserveStockUseCase;
import com.example.inventory.application.port.in.SkuNotFoundException;
import com.example.inventory.application.port.out.StockRepository;
import com.example.inventory.domain.StockItem;

import org.springframework.stereotype.Service;

@Service
class ReserveStockService implements ReserveStockUseCase {

    private final StockRepository stockRepository;

    ReserveStockService(StockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    @Override
    public ReservationResult reserve(ReserveStockCommand command) {
        StockItem item = stockRepository.findBySku(command.sku())
                .orElseThrow(() -> new SkuNotFoundException(command.sku()));

        item.reserve(command.quantity());
        stockRepository.save(item);

        return new ReservationResult(item.sku(), command.quantity(), item.available());
    }
}
