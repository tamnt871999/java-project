package com.example.inventory.exception;

public class InventoryNotFoundException extends RuntimeException {

    public InventoryNotFoundException(Long productId) {
        super("Khong tim thay ton kho cua san pham productId = " + productId);
    }
}
