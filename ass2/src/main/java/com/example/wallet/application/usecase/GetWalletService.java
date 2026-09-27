package com.example.wallet.application.usecase;

import com.example.wallet.application.port.in.GetWalletUseCase;
import com.example.wallet.application.port.in.WalletNotFoundException;
import com.example.wallet.application.port.in.WalletSnapshot;
import com.example.wallet.application.port.out.WalletRepository;
import com.example.wallet.domain.Wallet;
import com.example.wallet.domain.WalletId;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
class GetWalletService implements GetWalletUseCase {

    private final WalletRepository walletRepository;

    GetWalletService(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public WalletSnapshot getWallet(UUID walletId) {
        Wallet wallet = walletRepository.findById(WalletId.of(walletId))
                .orElseThrow(() -> new WalletNotFoundException(walletId));

        return WalletSnapshot.of(wallet);
    }
}
