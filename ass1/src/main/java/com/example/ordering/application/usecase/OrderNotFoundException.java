package com.example.ordering.application.usecase;

/**
 * Loi o tang USE CASE: du lieu tham chieu khong ton tai.
 *
 * Phan biet voi DomainException (quy tac nghiep vu bi vi pham) - hai loai loi
 * nay duoc Controller dich thanh hai ma HTTP khac nhau.
 */
public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(String orderId) {
        super("Khong tim thay don hang: " + orderId);
    }
}
