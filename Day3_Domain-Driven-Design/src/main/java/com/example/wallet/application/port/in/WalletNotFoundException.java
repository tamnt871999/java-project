package com.example.wallet.application.port.in;

import java.util.UUID;

public class WalletNotFoundException extends RuntimeException {

    public WalletNotFoundException(UUID walletId) {
        super("Khong tim thay vi dien tu: " + walletId);
    }
}
