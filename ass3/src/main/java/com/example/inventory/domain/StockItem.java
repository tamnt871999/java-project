package com.example.inventory.domain;

public final class StockItem {

    private final String sku;
    private int available;

    private StockItem(String sku, int available) {
        if (sku == null || sku.isBlank()) {
            throw new DomainException("SKU khong duoc rong");
        }
        if (available < 0) {
            throw new DomainException("Ton kho khong duoc am: " + available);
        }
        this.sku = sku;
        this.available = available;
    }

    public static StockItem rehydrate(String sku, int available) {
        return new StockItem(sku, available);
    }

    public void reserve(int quantity) {
        if (quantity <= 0) {
            throw new DomainException("So luong giu cho phai lon hon 0");
        }
        if (quantity > available) {
            throw new DomainException(
                    "Ton kho khong du: con " + available + ", can " + quantity);
        }
        available -= quantity;
    }

    public String sku() {
        return sku;
    }

    public int available() {
        return available;
    }
}
