package com.example.ordering.domain;

/** VALUE OBJECT so luong - tu chan gia tri <= 0 va vuot han muc mot lan mua. */
public record Quantity(int value) {

    public static final int MIN = 1;
    public static final int MAX = 100;

    public Quantity {
        if (value < MIN || value > MAX) {
            throw new DomainException(
                    "So luong phai trong khoang %d..%d, nhan duoc %d".formatted(MIN, MAX, value));
        }
    }

    public static Quantity of(int value) {
        return new Quantity(value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
