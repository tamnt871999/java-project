package com.example.wallet.legacy;

import java.math.BigDecimal;
import java.util.UUID;

public final class LegacyWalletCode {

    public static class WalletEntity {
        public UUID id;
        public BigDecimal balance;
        public String status;
    }

    public static class WalletService {

        public void withdraw(WalletEntity wallet, BigDecimal amount) throws Exception {
            if ("LOCKED".equals(wallet.status)) {
                throw new Exception("Vi dien tu hien dang bi khoa!");
            }
            wallet.balance = wallet.balance.subtract(amount);
        }
    }

    private LegacyWalletCode() {
    }
}
