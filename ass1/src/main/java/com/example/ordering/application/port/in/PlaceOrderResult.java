package com.example.ordering.application.port.in;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * DTO BIEN - du lieu di RA khoi use case.
 *
 * Dung hai truong nhu sequence diagram ve: Result(orderId, total).
 *
 * Co y KHONG tra thang aggregate Order ra ngoai. Neu tra, Controller se goi
 * duoc cac method cua domain, va moi thay doi ben trong aggregate lap tuc
 * thanh thay doi hop dong API.
 *
 * Cac con so trung gian (tam tinh, giam gia, phi ship) van duoc tinh va van
 * duoc luu xuong database, chung chi khong nam trong hop dong tra ve.
 */
public record PlaceOrderResult(String orderId, BigDecimal total) {

    public PlaceOrderResult {
        Objects.requireNonNull(orderId, "orderId must not be null");
        Objects.requireNonNull(total, "total must not be null");
    }
}
