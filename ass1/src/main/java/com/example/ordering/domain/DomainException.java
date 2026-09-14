package com.example.ordering.domain;

/**
 * Vi pham quy tac cua vong ENTITIES.
 *
 * Domain khong biet HTTP status hay ma loi SQL la gi. Viec dich exception nay
 * thanh 400 / 409 la trach nhiem cua vong Interface Adapters.
 */
public class DomainException extends RuntimeException {

    public DomainException(String message) {
        super(message);
    }
}
