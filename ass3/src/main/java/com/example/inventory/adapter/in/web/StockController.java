package com.example.inventory.adapter.in.web;

import com.example.inventory.application.port.in.GetStockUseCase;
import com.example.inventory.application.port.in.GetStockUseCase.StockView;
import com.example.inventory.application.port.in.ReserveStockUseCase;
import com.example.inventory.application.port.in.ReserveStockUseCase.ReservationResult;
import com.example.inventory.application.port.in.ReserveStockUseCase.ReserveStockCommand;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stock")
class StockController {

    private final ReserveStockUseCase reserveStockUseCase;
    private final GetStockUseCase getStockUseCase;

    StockController(ReserveStockUseCase reserveStockUseCase, GetStockUseCase getStockUseCase) {
        this.reserveStockUseCase = reserveStockUseCase;
        this.getStockUseCase = getStockUseCase;
    }

    @PostMapping("/{sku}/reserve")
    ReservationResult reserve(@PathVariable("sku") String sku,
                              @Valid @RequestBody ReserveRequest request) {
        return reserveStockUseCase.reserve(new ReserveStockCommand(sku, request.quantity()));
    }

    @GetMapping("/{sku}")
    StockView getStock(@PathVariable("sku") String sku) {
        return getStockUseCase.getStock(sku);
    }

    record ReserveRequest(@NotNull Integer quantity) {
    }
}
