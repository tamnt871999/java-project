package com.example.leaderboard.exception;

public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(Long id) {
        super("Khong tim thay san pham id = " + id);
    }
}
