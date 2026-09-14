package com.example.wallet.application.usecase;

import com.example.wallet.application.port.in.WalletSnapshot;
import com.example.wallet.application.port.in.WithdrawMoneyCommand;
import com.example.wallet.application.port.in.WithdrawMoneyUseCase;
import com.example.wallet.application.port.out.WalletRepository;
import com.example.wallet.domain.Wallet;
import com.example.wallet.domain.WalletId;

import java.util.Objects;

/**
 * INTERACTOR - day la thu con lai cua WalletService cu sau khi nghiep vu da
 * duoc don ve aggregate.
 *
 * Hay so sanh voi ban cu:
 *
 *   // CU - service vua dieu phoi vua phan xu nghiep vu
 *   if ("LOCKED".equals(wallet.status)) throw new Exception("...");
 *   wallet.balance = wallet.balance.subtract(amount);
 *
 *   // MOI - service chi dieu phoi
 *   wallet.withdrawMoney(command.amount());
 *
 * Use case gio chi con lam ba viec khong mang tinh nghiep vu: TIM vi, BAO vi
 * tu rut tien, LUU lai. Moi cau hoi "co duoc phep khong" deu do aggregate tra
 * loi. Do la ranh gioi giua tang Application va tang Domain.
 *
 * Vi the file nay khong co lay mot cau if nao ve nghiep vu - va do la dau hieu
 * cua mot rich domain model dung nghia.
 */
public class WithdrawMoneyService implements WithdrawMoneyUseCase {

    private final WalletRepository walletRepository;

    public WithdrawMoneyService(WalletRepository walletRepository) {
        this.walletRepository = Objects.requireNonNull(walletRepository,
                "walletRepository must not be null");
    }

    @Override
    public WalletSnapshot withdrawMoney(WithdrawMoneyCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        // 1. Tim aggregate.
        Wallet wallet = walletRepository.findById(WalletId.of(command.walletId()))
                .orElseThrow(() -> new WalletNotFoundException(command.walletId()));

        // 2. Bao aggregate tu lam viec cua no. Neu vi bi khoa hoac khong du so
        //    du, DomainException bay len tu day va use case KHONG bat lai - no
        //    khong co tham quyen phan xu luat nghiep vu.
        wallet.withdrawMoney(command.amount());

        // 3. Luu trang thai moi.
        walletRepository.save(wallet);

        return WalletSnapshots.of(wallet);
    }
}
