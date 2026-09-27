package com.example.inventory.application.port.in;

public interface ReserveStockUseCase {

    ReservationResult reserve(ReserveStockCommand command);

    record ReserveStockCommand(String sku, int quantity) {
    }

    record ReservationResult(String sku, int reserved, int available) {
    }
}
