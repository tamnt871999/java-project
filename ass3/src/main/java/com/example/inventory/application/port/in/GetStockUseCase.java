package com.example.inventory.application.port.in;

public interface GetStockUseCase {

    StockView getStock(String sku);

    record StockView(String sku, int available) {
    }
}
