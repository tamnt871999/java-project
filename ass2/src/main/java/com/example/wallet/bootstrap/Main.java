package com.example.wallet.bootstrap;

import com.example.wallet.adapter.out.persistence.InMemoryWalletRepository;
import com.example.wallet.application.port.in.LockWalletUseCase;
import com.example.wallet.application.port.in.WalletSnapshot;
import com.example.wallet.application.port.in.WithdrawMoneyCommand;
import com.example.wallet.application.port.in.WithdrawMoneyUseCase;
import com.example.wallet.application.port.out.WalletRepository;
import com.example.wallet.application.usecase.LockWalletService;
import com.example.wallet.application.usecase.WithdrawMoneyService;
import com.example.wallet.domain.DomainException;
import com.example.wallet.domain.Money;
import com.example.wallet.domain.Wallet;
import com.example.wallet.domain.WalletId;

import java.math.BigDecimal;

/**
 * COMPOSITION ROOT - noi duy nhat duoc phep biet ban hien thuc cu the.
 *
 * Trach nhiem duy nhat: lap rap cac tang roi chay thu ba tinh huong de thay
 * invariant hoat dong. Trong Spring Boot doan nay duoc thay bang @Component +
 * tiem tu dong, nhung y tuong khong doi: viec lap rap nam o ria, khong nam
 * trong loi.
 */
public final class Main {

    public static void main(String[] args) {
        // --- Lap rap --------------------------------------------------------
        WalletRepository walletRepository = new InMemoryWalletRepository();
        WithdrawMoneyUseCase withdrawMoney = new WithdrawMoneyService(walletRepository);
        LockWalletUseCase lockWallet = new LockWalletService(walletRepository);

        // --- Du lieu mau ----------------------------------------------------
        WalletId walletId = WalletId.random();
        walletRepository.save(Wallet.open(walletId, Money.of("1000.00")));

        System.out.println();
        System.out.println("=== VI DIEN TU: ANEMIC -> RICH DOMAIN MODEL ===");
        System.out.println();
        System.out.println("Vi " + walletId + " mo voi so du 1000.00");
        System.out.println();

        // 1. Rut hop le.
        show("Rut 300.00", () -> withdrawMoney.withdrawMoney(
                new WithdrawMoneyCommand(walletId.value(), new BigDecimal("300.00"))));

        // 2. INVARIANT so du - loi ma ban cu de lot.
        show("Rut 5000.00 (qua so du)", () -> withdrawMoney.withdrawMoney(
                new WithdrawMoneyCommand(walletId.value(), new BigDecimal("5000.00"))));

        // 3. So tien khong hop le.
        show("Rut -100.00 (so am)", () -> withdrawMoney.withdrawMoney(
                new WithdrawMoneyCommand(walletId.value(), new BigDecimal("-100.00"))));

        // 4. Khoa vi.
        show("Khoa vi", () -> lockWallet.lockWallet(walletId.value()));

        // 5. INVARIANT trang thai.
        show("Rut 100.00 sau khi khoa", () -> withdrawMoney.withdrawMoney(
                new WithdrawMoneyCommand(walletId.value(), new BigDecimal("100.00"))));

        System.out.println();
        System.out.println("Nhan xet: ca ba lan bi tu choi deu do chinh aggregate Wallet");
        System.out.println("phan xu, khong phai do use case kiem tra ho. Khong co duong");
        System.out.println("nao khac de so du thay doi, nen vi khong the tut xuong am.");
        System.out.println();
    }

    /** Chay mot thao tac roi in ket qua hoac ly do bi tu choi. */
    private static void show(String label, Operation operation) {
        try {
            WalletSnapshot snapshot = operation.run();
            System.out.printf("  %-28s OK      so du = %s, trang thai = %s%n",
                    label, snapshot.balance().toPlainString(), snapshot.status());
        } catch (DomainException e) {
            System.out.printf("  %-28s TU CHOI %s%n", label, e.getMessage());
        }
    }

    @FunctionalInterface
    private interface Operation {
        WalletSnapshot run();
    }

    private Main() {
    }
}
