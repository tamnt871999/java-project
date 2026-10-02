package com.example.wallet.application.usecase;

import com.example.wallet.application.port.in.OpenWalletUseCase;
import com.example.wallet.application.port.in.WalletSnapshot;
import com.example.wallet.application.port.out.WalletRepository;
import com.example.wallet.domain.Money;
import com.example.wallet.domain.Wallet;
import com.example.wallet.domain.WalletId;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class OpenWalletService implements OpenWalletUseCase {

    private final WalletRepository walletRepository;

    OpenWalletService(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    @Override
    @Transactional
    public WalletSnapshot openWallet(OpenWalletCommand command) {
        Wallet wallet = Wallet.open(WalletId.random(), new Money(command.initialBalance()));
        walletRepository.save(wallet);
        return WalletSnapshot.of(wallet);
    }
}
