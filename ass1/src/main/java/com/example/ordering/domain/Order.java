package com.example.ordering.domain;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * AGGREGATE ROOT - vong trong cung cua Clean Architecture.
 *
 * Khong import Spring, khong import JPA, khong import Jackson. Doi framework
 * hay doi database deu khong cham toi file nay - do chinh la muc dich cua
 * Dependency Rule.
 *
 * Moi thay doi trang thai deu di qua method co nghia nghiep vu (place, cancel),
 * khong co setter cong khai. Aggregate tu bao ve invariant cua chinh no.
 */
public class Order {

    /** Chua co ma don cho toi khi duoc luu - xem withId(). */
    private final OrderId id;
    private final CustomerId customerId;
    private final List<OrderItem> items;
    private final PriceBreakdown price;
    private final Instant placedAt;
    private final OrderStatus status;

    private Order(OrderId id, CustomerId customerId, List<OrderItem> items,
                  PriceBreakdown price, OrderStatus status, Instant placedAt) {
        this.id = id;
        this.customerId = customerId;
        this.items = List.copyOf(items);
        this.price = price;
        this.status = status;
        this.placedAt = placedAt;
    }

    /**
     * FACTORY METHOD: dat mot don hang moi.
     *
     * Toan bo dinh nghia "the nao la mot don hang hop le" nam o day - khong nam
     * o Use Case va cang khong nam o Controller.
     */
    public static Order place(CustomerId customerId, List<OrderItem> items,
                              PriceBreakdown price, Instant placedAt) {
        Objects.requireNonNull(customerId, "customerId must not be null");
        Objects.requireNonNull(items, "items must not be null");
        Objects.requireNonNull(price, "price must not be null");
        Objects.requireNonNull(placedAt, "placedAt must not be null");

        if (items.isEmpty()) {
            throw new DomainException("Don hang phai co it nhat mot dong hang");
        }
        requireNoDuplicateProduct(items);

        // Chua co ma don: ma don do tang luu tru sinh ra, giong @GeneratedValue.
        return new Order(null, customerId, items, price, OrderStatus.PLACED, placedAt);
    }

    /**
     * Dung lai aggregate tu du lieu da luu.
     *
     * Tach rieng khoi place() de viec khoi phuc khong chay lai quy tac cua viec
     * tao moi - gia da chot hom qua khong bi tinh lai theo chinh sach hom nay.
     */
    public static Order rehydrate(OrderId id, CustomerId customerId, List<OrderItem> items,
                                  PriceBreakdown price, OrderStatus status, Instant placedAt) {
        return new Order(id, customerId, items, price, status, placedAt);
    }

    /**
     * Tra ve BAN SAO cua don hang kem ma don vua duoc cap.
     *
     * Aggregate van bat bien: khong sua tai cho ma tao vat the moi. Tang luu
     * tru goi ham nay ngay truoc khi ghi xuong database.
     */
    public Order withId(OrderId assignedId) {
        Objects.requireNonNull(assignedId, "assignedId must not be null");
        if (this.id != null) {
            throw new DomainException("Don hang da co ma don: " + this.id);
        }
        return new Order(assignedId, customerId, items, price, status, placedAt);
    }

    private static void requireNoDuplicateProduct(List<OrderItem> items) {
        Set<ProductId> seen = new HashSet<>();
        for (OrderItem item : items) {
            if (!seen.add(item.productId())) {
                throw new DomainException("San pham bi lap lai trong don hang: " + item.productId());
            }
        }
    }

    /** @throws DomainException neu don hang chua duoc luu nen chua co ma don */
    public OrderId id() {
        if (id == null) {
            throw new DomainException("Don hang chua duoc luu nen chua co ma don");
        }
        return id;
    }

    public CustomerId customerId() {
        return customerId;
    }

    /** Danh sach bat bien: khong ai sua duoc ruot aggregate tu ben ngoai. */
    public List<OrderItem> items() {
        return items;
    }

    public PriceBreakdown price() {
        return price;
    }

    public OrderStatus status() {
        return status;
    }

    public Instant placedAt() {
        return placedAt;
    }

    public int totalItems() {
        return items.stream().mapToInt(item -> item.quantity().value()).sum();
    }

    @Override
    public boolean equals(Object other) {
        // Entity so sanh theo dinh danh, khong theo thuoc tinh. Don chua duoc
        // luu thi chua co dinh danh nen chi bang chinh no.
        if (this == other) {
            return true;
        }
        return other instanceof Order order && id != null && id.equals(order.id);
    }

    @Override
    public int hashCode() {
        return id == null ? System.identityHashCode(this) : id.hashCode();
    }

    @Override
    public String toString() {
        return "Order[id=%s, customer=%s, status=%s, total=%s]"
                .formatted(id == null ? "chua cap" : id, customerId, status, price.total());
    }
}
