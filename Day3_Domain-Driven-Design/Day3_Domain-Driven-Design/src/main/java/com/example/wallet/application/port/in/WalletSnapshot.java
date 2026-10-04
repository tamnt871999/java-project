package com.example.wallet.application.port.in;

import com.example.wallet.domain.Wallet;

import java.math.BigDecimal;
import java.util.UUID;

public record WalletSnapshot(UUID walletId, BigDecimal balance, String status) {

    public static WalletSnapshot of(Wallet wallet) {
        return new WalletSnapshot(
                wallet.id().value(),
                wallet.balance().amount(),
                wallet.status().name());
    }
}
