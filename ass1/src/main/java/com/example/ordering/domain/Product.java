package com.example.ordering.domain;

import java.util.Objects;

/**
 * MODEL - san pham trong catalog.
 *
 * Trong MVC, Model khong chi la "cuc du lieu": no van giu nhung hanh vi
 * gan lien voi chinh no, o day la viec tru va cong ton kho.
 */
public class Product {

    private final String id;
    private final String name;
    private final Money price;
    private int stock;

    public Product(String id, String name, Money price, int stock) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.price = Objects.requireNonNull(price, "price must not be null");
        this.stock = stock;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Money getPrice() {
        return price;
    }

    public int getStock() {
        return stock;
    }

    public boolean isInStock() {
        return stock > 0;
    }

    public boolean hasEnoughStock(int quantity) {
        return stock >= quantity;
    }

    /** Tru kho khi ban hang. Model tu bao ve khong cho ton kho am. */
    public void decreaseStock(int quantity) {
        if (quantity <= 0) {
            throw new DomainException("So luong tru kho phai lon hon 0");
        }
        if (quantity > stock) {
            throw new DomainException(
                    "Khong du ton kho cho " + name + ": can " + quantity + ", con " + stock);
        }
        this.stock -= quantity;
    }

    /** Hoan kho khi huy don. */
    public void increaseStock(int quantity) {
        if (quantity <= 0) {
            throw new DomainException("So luong hoan kho phai lon hon 0");
        }
        this.stock += quantity;
    }

    @Override
    public String toString() {
        return name + " (" + id + ") - " + price;
    }
}
