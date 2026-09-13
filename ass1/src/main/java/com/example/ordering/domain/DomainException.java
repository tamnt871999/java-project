package com.example.ordering.domain;

/** Loi do vi pham rang buoc cua chinh Model (du lieu khong hop le). */
public class DomainException extends RuntimeException {

    public DomainException(String message) {
        super(message);
    }
}
