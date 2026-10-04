package com.example.wallet.application.port.in;

import java.math.BigDecimal;
import java.util.UUID;

public interface WithdrawMoneyUseCase {

    WalletSnapshot withdrawMoney(WithdrawMoneyCommand command);

    record WithdrawMoneyCommand(UUID walletId, BigDecimal amount) {
    }
}
