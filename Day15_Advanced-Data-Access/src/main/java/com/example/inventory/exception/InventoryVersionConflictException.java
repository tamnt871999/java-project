package com.example.inventory.exception;

public class InventoryVersionConflictException extends RuntimeException {

    public InventoryVersionConflictException(Long productId, long expectedVersion, long currentVersion) {
        super("San pham productId = " + productId + " da bi nguoi khac sua: ban gui version "
                + expectedVersion + ", hien tai la version " + currentVersion);
    }
}
