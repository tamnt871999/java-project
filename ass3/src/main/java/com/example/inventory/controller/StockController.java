package com.example.inventory.controller;

import com.example.inventory.service.StockService;
import com.example.inventory.service.StockService.Reservation;
import com.example.inventory.service.StockService.StockView;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stock")
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    @PostMapping("/{sku}/reserve")
    public Reservation reserve(@PathVariable("sku") String sku,
                               @RequestHeader(value = "X-Node", required = false) String node,
                               @RequestHeader(value = "X-Strategy", required = false) String strategy,
                               @Valid @RequestBody ReserveRequest request) {
        return stockService.reserve(strategy, node, sku, request.quantity());
    }

    @GetMapping("/{sku}")
    public StockView getStock(@PathVariable("sku") String sku,
                              @RequestHeader(value = "X-Node", required = false) String node,
                              @RequestHeader(value = "X-Strategy", required = false) String strategy) {
        return stockService.getStock(strategy, node, sku);
    }

    public record ReserveRequest(@NotNull Integer quantity) {
    }
}
