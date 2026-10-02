package com.example.ordering.adapter.out.persistence;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
class OrderJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @Column(name = "placed_at", nullable = false)
    private Instant placedAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "order_items", joinColumns = @JoinColumn(name = "order_id"))
    private List<ItemEmbeddable> items = new ArrayList<>();

    protected OrderJpaEntity() {
    }

    OrderJpaEntity(String customerId, Instant placedAt, List<ItemEmbeddable> items) {
        this.customerId = customerId;
        this.placedAt = placedAt;
        this.items = new ArrayList<>(items);
    }

    Long getId() {
        return id;
    }

    String getCustomerId() {
        return customerId;
    }

    Instant getPlacedAt() {
        return placedAt;
    }

    List<ItemEmbeddable> getItems() {
        return items;
    }

    @Embeddable
    static class ItemEmbeddable {

        @Column(name = "product_id", nullable = false)
        private String productId;

        @Column(name = "quantity", nullable = false)
        private int quantity;

        @Column(name = "unit_price", nullable = false, precision = 19, scale = 2)
        private BigDecimal unitPrice;

        protected ItemEmbeddable() {
        }

        ItemEmbeddable(String productId, int quantity, BigDecimal unitPrice) {
            this.productId = productId;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
        }

        String getProductId() {
            return productId;
        }

        int getQuantity() {
            return quantity;
        }

        BigDecimal getUnitPrice() {
            return unitPrice;
        }
    }
}
