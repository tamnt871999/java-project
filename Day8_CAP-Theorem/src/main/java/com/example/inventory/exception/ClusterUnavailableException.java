package com.example.inventory.exception;

public class ClusterUnavailableException extends RuntimeException {

    public ClusterUnavailableException(String message) {
        super(message);
    }
}
