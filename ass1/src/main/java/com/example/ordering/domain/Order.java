package com.example.ordering.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * MODEL trung tam cua ung dung.
 *
 * Trong MVC/layered, Model giu DU LIEU va cac phep tinh gan lien voi du lieu
 * do (subtotal, total). Con CHINH SACH kinh doanh - giam gia bao nhieu, khi nao
 * mien phi ship - nam o tang Service (xem OrderService.calculatePricing).
 *
 * Day la diem khac ro nhat so voi ban Hexagonal, noi moi quy tac deu bi don
 * vao trong aggregate. Ca hai cach deu dung, mien la ban BIET minh chon gi.
 */
public class Order {

    private final String id;
    private final String customerId;
    private final String customerName;
    private final String shippingAddress;
    private final List<OrderLine> lines = new ArrayList<>();
    private final LocalDateTime createdAt;

    private OrderStatus status = OrderStatus.NEW;
    private Money discount = Money.ZERO;
    private Money shippingFee = Money.ZERO;
    private String note;

    public Order(String id, String customerId, String customerName,
                 String shippingAddress, LocalDateTime createdAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.customerId = Objects.requireNonNull(customerId, "customerId must not be null");
        this.customerName = Objects.requireNonNull(customerName, "customerName must not be null");
        this.shippingAddress = Objects.requireNonNull(shippingAddress, "shippingAddress must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public void addLine(OrderLine line) {
        Objects.requireNonNull(line, "line must not be null");
        boolean duplicated = lines.stream()
                .anyMatch(existing -> existing.getProductId().equals(line.getProductId()));
        if (duplicated) {
            throw new DomainException("San pham da co trong don hang: " + line.getProductName());
        }
        lines.add(line);
    }

    /** Service goi vao sau khi tinh xong chinh sach gia. */
    public void applyPricing(Money discount, Money shippingFee) {
        this.discount = Objects.requireNonNull(discount, "discount must not be null");
        this.shippingFee = Objects.requireNonNull(shippingFee, "shippingFee must not be null");
    }

    public void markPaid() {
        if (status != OrderStatus.NEW) {
            throw new DomainException("Khong the thanh toan don o trang thai " + status.label());
        }
        this.status = OrderStatus.PAID;
    }

    public void cancel(String reason) {
        if (status == OrderStatus.CANCELLED) {
            throw new DomainException("Don hang da bi huy truoc do");
        }
        this.status = OrderStatus.CANCELLED;
        this.note = reason;
    }

    public boolean isEmpty() {
        return lines.isEmpty();
    }

    public Money getSubtotal() {
        return lines.stream().map(OrderLine::getLineTotal).reduce(Money.ZERO, Money::plus);
    }

    public Money getTotal() {
        return getSubtotal().minus(discount).plus(shippingFee);
    }

    public int getTotalItems() {
        return lines.stream().mapToInt(OrderLine::getQuantity).sum();
    }

    public String getId() {
        return id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    /** Tra ve danh sach chi doc: View chi duoc DOC Model, khong duoc sua. */
    public List<OrderLine> getLines() {
        return Collections.unmodifiableList(lines);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public Money getDiscount() {
        return discount;
    }

    public Money getShippingFee() {
        return shippingFee;
    }

    public String getNote() {
        return note;
    }

    @Override
    public String toString() {
        return "Order " + id + " [" + status.label() + "] " + getTotal();
    }
}
