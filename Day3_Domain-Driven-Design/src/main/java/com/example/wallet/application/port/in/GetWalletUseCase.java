package com.example.wallet.application.port.in;

import java.util.UUID;

public interface GetWalletUseCase {

    WalletSnapshot getWallet(UUID walletId);
}
