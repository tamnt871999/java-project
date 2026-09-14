package com.example.ordering.domain;

import java.util.Objects;

/** VALUE OBJECT dinh danh san pham. */
public record ProductId(String value) {

    public ProductId {
        Objects.requireNonNull(value, "ProductId value must not be null");
        if (value.isBlank()) {
            throw new DomainException("ProductId khong duoc rong");
        }
    }

    public static ProductId of(String value) {
        return new ProductId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
