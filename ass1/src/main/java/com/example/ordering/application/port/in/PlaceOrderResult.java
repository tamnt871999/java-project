package com.example.ordering.application.port.in;

import java.math.BigDecimal;

/**
 * DTO BIEN - du lieu di RA khoi use case.
 *
 * Co y KHONG tra thang aggregate Order ra ngoai: neu tra, Controller se goi
 * duoc cac method doi trang thai cua domain, va moi thay doi ben trong
 * aggregate lap tuc thanh thay doi hop dong API.
 *
 * Day chinh la "201 Created {orderId, total}" o cuoi sequence diagram.
 */
public record PlaceOrderResult(String orderId,
                               String customerId,
                               String status,
                               BigDecimal subtotal,
                               BigDecimal discount,
                               BigDecimal shippingFee,
                               BigDecimal total,
                               int totalItems,
                               String placedAt) {
}
