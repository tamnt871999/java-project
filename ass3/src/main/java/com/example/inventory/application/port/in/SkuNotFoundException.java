package com.example.inventory.application.port.in;

public class SkuNotFoundException extends RuntimeException {

    public SkuNotFoundException(String sku) {
        super("Khong tim thay SKU " + sku);
    }
}
