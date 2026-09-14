package com.example.ordering.domain;

import java.util.Objects;

/** VALUE OBJECT dinh danh khach hang. */
public record CustomerId(String value) {

    public CustomerId {
        Objects.requireNonNull(value, "CustomerId value must not be null");
        if (value.isBlank()) {
            throw new DomainException("CustomerId khong duoc rong");
        }
    }

    public static CustomerId of(String value) {
        return new CustomerId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
