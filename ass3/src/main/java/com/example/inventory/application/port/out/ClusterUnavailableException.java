package com.example.inventory.application.port.out;

public class ClusterUnavailableException extends RuntimeException {

    public ClusterUnavailableException(String message) {
        super(message);
    }
}
