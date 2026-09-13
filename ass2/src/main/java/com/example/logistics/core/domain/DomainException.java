package com.example.logistics.core.domain;

/** Loi do vi pham rang buoc cua chinh Domain (du lieu dau vao khong hop le). */
public class DomainException extends RuntimeException {

    public DomainException(String message) {
        super(message);
    }
}
