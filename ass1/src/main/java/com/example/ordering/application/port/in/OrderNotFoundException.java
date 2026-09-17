package com.example.ordering.application.port.in;

/**
 * Loi cua tang APPLICATION, khong phai cua domain.
 *
 * Phan biet cho nay rat de nham:
 *   DomainException        - quy tac nghiep vu bi vi pham (don rong, san pham lap)
 *   OrderNotFoundException - nghiep vu khong sai, chi la du lieu khong ton tai
 *
 * Vi the no nam o application chu khong nam o domain: aggregate Order khong he
 * biet den khai niem "tim khong thay", do la chuyen cua nguoi di tim.
 *
 * Va no KHONG mang so 404. Use case khong duoc biet HTTP ton tai; viec dich
 * exception nay thanh 404 la trach nhiem cua OrderController.
 */
public class OrderNotFoundException extends RuntimeException {

    private final String orderId;

    public OrderNotFoundException(String orderId) {
        super("Khong tim thay don hang: " + orderId);
        this.orderId = orderId;
    }

    public String orderId() {
        return orderId;
    }
}
