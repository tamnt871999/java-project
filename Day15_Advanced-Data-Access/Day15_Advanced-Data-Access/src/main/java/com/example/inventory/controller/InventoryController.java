package com.example.inventory.controller;

import com.example.inventory.service.InventoryService;
import com.example.inventory.service.InventoryService.InventoryView;
import com.example.inventory.service.InventoryService.QuantityChange;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/inventories")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping("/{productId}/stock-in")
    public InventoryView stockIn(@PathVariable("productId") Long productId,
                                 @Valid @RequestBody StockInRequest request) {
        return inventoryService.stockIn(productId, request.quantity());
    }

    @PutMapping("/{productId}")
    public InventoryView update(@PathVariable("productId") Long productId,
                                @Valid @RequestBody UpdateInventoryRequest request) {
        return inventoryService.update(productId, request.productName(), request.version());
    }

    @GetMapping("/{productId}/history")
    public List<QuantityChange> history(@PathVariable("productId") Long productId) {
        return inventoryService.quantityHistory(productId);
    }

    public record StockInRequest(@NotNull @Positive Integer quantity) {
    }

    public record UpdateInventoryRequest(@NotBlank String productName, @NotNull Long version) {
    }
}
