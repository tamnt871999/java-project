package com.example.wallet.domain;

import java.util.Objects;
import java.util.UUID;

public record WalletId(UUID value) {

    public WalletId {
        Objects.requireNonNull(value, "WalletId value must not be null");
    }

    public static WalletId of(UUID value) {
        return new WalletId(value);
    }

    public static WalletId random() {
        return new WalletId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
