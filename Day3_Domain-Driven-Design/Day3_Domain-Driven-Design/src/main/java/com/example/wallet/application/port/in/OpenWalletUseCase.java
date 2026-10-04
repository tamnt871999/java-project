package com.example.wallet.application.port.in;

import java.math.BigDecimal;

public interface OpenWalletUseCase {

    WalletSnapshot openWallet(OpenWalletCommand command);

    record OpenWalletCommand(BigDecimal initialBalance) {
    }
}
