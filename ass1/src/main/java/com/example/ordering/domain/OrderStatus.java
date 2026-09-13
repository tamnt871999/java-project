package com.example.ordering.domain;

/** Vong doi don hang: NEW -> PAID hoac NEW -> CANCELLED. */
public enum OrderStatus {

    NEW("Cho thanh toan"),
    PAID("Da thanh toan"),
    CANCELLED("Da huy");

    private final String label;

    OrderStatus(String label) {
        this.label = label;
    }

    /** Nhan tieng Viet de View hien thi thang, khong phai tu map lai. */
    public String label() {
        return label;
    }
}
