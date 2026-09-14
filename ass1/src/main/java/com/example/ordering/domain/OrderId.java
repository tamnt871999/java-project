package com.example.ordering.domain;

import java.util.Objects;

/**
 * VALUE OBJECT dinh danh don hang.
 *
 * Dung kieu rieng thay cho String tran de compiler chan loi truyen nham id
 * (vi du truyen CustomerId vao cho can OrderId).
 */
public record OrderId(String value) {

    public OrderId {
        Objects.requireNonNull(value, "OrderId value must not be null");
        if (value.isBlank()) {
            throw new DomainException("OrderId khong duoc rong");
        }
    }

    public static OrderId of(String value) {
        return new OrderId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
