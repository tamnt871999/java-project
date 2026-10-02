package com.example.wallet.application.usecase;

import com.example.wallet.application.port.in.WalletNotFoundException;
import com.example.wallet.application.port.in.WalletSnapshot;
import com.example.wallet.application.port.in.WithdrawMoneyUseCase;
import com.example.wallet.application.port.out.WalletRepository;
import com.example.wallet.domain.Wallet;
import com.example.wallet.domain.WalletId;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class WithdrawMoneyService implements WithdrawMoneyUseCase {

    private final WalletRepository walletRepository;

    WithdrawMoneyService(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    @Override
    @Transactional
    public WalletSnapshot withdrawMoney(WithdrawMoneyCommand command) {
        Wallet wallet = walletRepository.findById(WalletId.of(command.walletId()))
                .orElseThrow(() -> new WalletNotFoundException(command.walletId()));

        wallet.withdrawMoney(command.amount());
        walletRepository.save(wallet);

        return WalletSnapshot.of(wallet);
    }
}
