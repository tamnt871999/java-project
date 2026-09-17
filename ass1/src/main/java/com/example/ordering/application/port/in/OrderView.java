package com.example.ordering.application.port.in;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * DTO BIEN - du lieu di RA khoi use case DOC.
 *
 * Chu y no KHAC PlaceOrderResult, du ca hai cung mo ta mot don hang:
 *
 *   PlaceOrderResult : {orderId, total}        - tra loi "da dat xong, ma nao"
 *   OrderView        : day du chi tiet don     - tra loi "don nay trong the nao"
 *
 * Day khong phai trung lap ma la co y. Mot DTO dung chung cho ca ghi lan doc
 * se bi keo cang theo hai huong: them truong cho man hinh chi tiet la lap tuc
 * lam phinh response cua API tao don. Tach ra thi moi ben tien hoa doc lap -
 * day chinh la y tuong nen tang cua CQRS.
 *
 * Van la record thuan, khong annotation: adapter muon doi ten truong tren JSON
 * thi sua o OrderJsonMapper, khong sua o day.
 */
public record OrderView(String orderId,
                        String customerId,
                        String status,
                        String placedAt,
                        List<Line> items,
                        BigDecimal subtotal,
                        BigDecimal discount,
                        BigDecimal shippingFee,
                        BigDecimal total) {

    public OrderView {
        Objects.requireNonNull(orderId, "orderId must not be null");
        Objects.requireNonNull(items, "items must not be null");
        items = List.copyOf(items);
    }

    /** Mot dong hang khi doc lai don. */
    public record Line(String productId, int quantity, BigDecimal unitPrice, BigDecimal lineTotal) {
    }
}
