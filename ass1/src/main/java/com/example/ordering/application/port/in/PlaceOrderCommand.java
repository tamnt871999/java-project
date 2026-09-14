package com.example.ordering.application.port.in;

import java.util.List;
import java.util.Objects;

/**
 * DTO BIEN - du lieu di VAO use case.
 *
 * Chi chua kieu nguyen thuy: day la hop dong voi the gioi ben ngoai, khong nen
 * bat adapter phai biet Value Object cua domain de goi duoc use case.
 * Viec dung ProductId / Quantity / Money la chuyen rieng cua ben trong.
 *
 * Command tu kiem tra CU PHAP ngay tai constructor (co mat truong khong, danh
 * sach co rong khong). Con kiem tra NGHIEP VU (gia tri hop le khong) la viec
 * cua domain.
 */
public record PlaceOrderCommand(String customerId, List<Item> items) {

    public PlaceOrderCommand {
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("Thieu customerId");
        }
        Objects.requireNonNull(items, "items must not be null");
        if (items.isEmpty()) {
            throw new IllegalArgumentException("Danh sach items khong duoc rong");
        }
        items = List.copyOf(items);
    }

    /** Mot dong hang trong yeu cau dat hang. */
    public record Item(String productId, int quantity, String unitPrice) {

        public Item {
            if (productId == null || productId.isBlank()) {
                throw new IllegalArgumentException("Thieu productId");
            }
            if (unitPrice == null || unitPrice.isBlank()) {
                throw new IllegalArgumentException("Thieu unitPrice cho " + productId);
            }
        }
    }
}
