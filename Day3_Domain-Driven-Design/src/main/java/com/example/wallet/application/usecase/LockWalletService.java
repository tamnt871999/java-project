package com.example.wallet.application.usecase;

import com.example.wallet.application.port.in.LockWalletUseCase;
import com.example.wallet.application.port.in.WalletNotFoundException;
import com.example.wallet.application.port.in.WalletSnapshot;
import com.example.wallet.application.port.out.WalletRepository;
import com.example.wallet.domain.Wallet;
import com.example.wallet.domain.WalletId;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
class LockWalletService implements LockWalletUseCase {

    private final WalletRepository walletRepository;

    LockWalletService(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    @Override
    @Transactional
    public WalletSnapshot lockWallet(UUID walletId) {
        Wallet wallet = walletRepository.findById(WalletId.of(walletId))
                .orElseThrow(() -> new WalletNotFoundException(walletId));

        wallet.lockWallet();
        walletRepository.save(wallet);

        return WalletSnapshot.of(wallet);
    }
}
