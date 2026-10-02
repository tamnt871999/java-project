package com.example.wallet.domain;

public enum WalletStatus {

    ACTIVE,
    LOCKED;

    public boolean isLocked() {
        return this == LOCKED;
    }
}
