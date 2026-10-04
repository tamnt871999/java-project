package com.example.wallet.application.port.in;

import java.util.UUID;

public interface LockWalletUseCase {

    WalletSnapshot lockWallet(UUID walletId);
}
