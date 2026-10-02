package com.example.inventory.exception;

public class SkuNotFoundException extends RuntimeException {

    public SkuNotFoundException(String sku) {
        super("Khong tim thay SKU " + sku);
    }
}
