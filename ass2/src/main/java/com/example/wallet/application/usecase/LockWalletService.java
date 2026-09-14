package com.example.wallet.application.usecase;

import com.example.wallet.application.port.in.LockWalletUseCase;
import com.example.wallet.application.port.in.WalletSnapshot;
import com.example.wallet.application.port.out.WalletRepository;
import com.example.wallet.domain.Wallet;
import com.example.wallet.domain.WalletId;

import java.util.Objects;
import java.util.UUID;

/**
 * INTERACTOR - khoa vi.
 *
 * Cung mot khuon voi WithdrawMoneyService: tim - bao aggregate lam - luu.
 * Khong co cau if nghiep vu nao, vi luat "vi da khoa roi thi khong khoa nua"
 * nam trong Wallet.lockWallet().
 */
public class LockWalletService implements LockWalletUseCase {

    private final WalletRepository walletRepository;

    public LockWalletService(WalletRepository walletRepository) {
        this.walletRepository = Objects.requireNonNull(walletRepository,
                "walletRepository must not be null");
    }

    @Override
    public WalletSnapshot lockWallet(UUID walletId) {
        Objects.requireNonNull(walletId, "walletId must not be null");

        Wallet wallet = walletRepository.findById(WalletId.of(walletId))
                .orElseThrow(() -> new WalletNotFoundException(walletId));

        wallet.lockWallet();
        walletRepository.save(wallet);

        return WalletSnapshots.of(wallet);
    }
}
