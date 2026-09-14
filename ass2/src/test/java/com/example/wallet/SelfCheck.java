package com.example.wallet;

import com.example.wallet.adapter.out.persistence.InMemoryWalletRepository;
import com.example.wallet.application.port.in.LockWalletUseCase;
import com.example.wallet.application.port.in.WalletSnapshot;
import com.example.wallet.application.port.in.WithdrawMoneyCommand;
import com.example.wallet.application.port.in.WithdrawMoneyUseCase;
import com.example.wallet.application.port.out.WalletRepository;
import com.example.wallet.application.usecase.LockWalletService;
import com.example.wallet.application.usecase.WalletNotFoundException;
import com.example.wallet.application.usecase.WithdrawMoneyService;
import com.example.wallet.domain.DomainException;
import com.example.wallet.domain.Money;
import com.example.wallet.domain.Wallet;
import com.example.wallet.domain.WalletId;
import com.example.wallet.domain.WalletStatus;
import com.example.wallet.legacy.LegacyWalletCode;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Bo kiem thu tu viet - khong dung JUnit de bai tap chay duoc chi voi JDK 21.
 *
 * Diem dang chu y: phan lon bai test duoi day cham THANG vao aggregate, khong
 * qua use case va khong qua tang luu tru. Do la phan thuong cua rich domain
 * model - luat nghiep vu nam trong mot doi tuong thuan Java nen kiem thu no
 * khong can dung bat ky ha tang nao.
 */
public final class SelfCheck {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        // Yeu cau a - dong goi
        khongConSetterCongKhai();
        thuocTinhLaPrivate();

        // Yeu cau b - phuong thuc giau hanh vi
        rutTienHopLeTruDungSoDu();
        khoaViDoiTrangThai();

        // Yeu cau c - invariant
        khongDuocRutQuaSoDu();
        khongDuocRutKhiViBiKhoa();
        khongDuocRutSoTienKhongHopLe();
        luatBatBienChanDuocLoiCuaBanCu();

        // Tang Application
        useCaseChiDieuPhoiVaTraDto();
        khongTimThayViLaLoiUngDungChuKhongPhaiLoiNghiepVu();
        aggregateBiTuChoiThiKhongGhiXuongKho();

        ArchitectureFitness.run();

        System.out.println();
        System.out.println("Ket qua: " + passed + " dat / " + failed + " loi");
        if (failed > 0) {
            System.exit(1);
        }
    }

    // --- YEU CAU a: DONG GOI ------------------------------------------------

    /**
     * Quet toan bo method cong khai cua Wallet de chac chan khong con setter.
     *
     * Kiem bang phan chieu chu khong doc bang mat: mot setter duoc them lai sau
     * nay se lam bai test nay do ngay, con mat nguoi thi bo qua de dang.
     */
    private static void khongConSetterCongKhai() {
        StringBuilder found = new StringBuilder();
        for (Method method : Wallet.class.getDeclaredMethods()) {
            if (!Modifier.isPublic(method.getModifiers())) {
                continue;
            }
            if (method.getName().startsWith("set")) {
                found.append(method.getName()).append(" ");
            }
        }
        check("Wallet khong co setter cong khai", "", found.toString().trim());
    }

    /** balance va status phai la private - khong ai sua duoc tu ben ngoai. */
    private static void thuocTinhLaPrivate() {
        for (String field : new String[] {"balance", "status", "id"}) {
            try {
                int modifiers = Wallet.class.getDeclaredField(field).getModifiers();
                check("truong '" + field + "' la private", true, Modifier.isPrivate(modifiers));
            } catch (NoSuchFieldException e) {
                fail("khong tim thay truong '" + field + "' tren Wallet");
            }
        }
    }

    // --- YEU CAU b: PHUONG THUC GIAU HANH VI --------------------------------

    private static void rutTienHopLeTruDungSoDu() {
        Wallet wallet = activeWallet("1000.00");
        wallet.withdrawMoney(new BigDecimal("300.00"));
        check("rut 300 tu 1000 con 700", Money.of("700.00"), wallet.balance());
        check("vi van ACTIVE sau khi rut", WalletStatus.ACTIVE, wallet.status());
    }

    private static void khoaViDoiTrangThai() {
        Wallet wallet = activeWallet("1000.00");
        wallet.lockWallet();
        check("khoa vi doi trang thai sang LOCKED", WalletStatus.LOCKED, wallet.status());
        check("khoa vi khong dong vao so du", Money.of("1000.00"), wallet.balance());

        // De bai khong dat luat nao cho viec khoa lai lan nua, nen aggregate
        // khong duoc tu nghi ra mot luat. Kiem tra day la kiem tra KHONG co
        // ngoai le nao bat ra, chu khong phai them mot invariant moi.
        wallet.lockWallet();
        check("khoa lai lan nua van LOCKED, khong nem loi", WalletStatus.LOCKED, wallet.status());
    }

    // --- YEU CAU c: INVARIANT -----------------------------------------------

    /** Chinh la luat ma ban cu bo quen. */
    private static void khongDuocRutQuaSoDu() {
        Wallet wallet = activeWallet("1000.00");
        expectDomainException("khong duoc rut qua so du",
                () -> wallet.withdrawMoney(new BigDecimal("1000.01")));
        check("so du khong doi khi thao tac bi tu choi", Money.of("1000.00"), wallet.balance());
    }

    private static void khongDuocRutKhiViBiKhoa() {
        Wallet wallet = activeWallet("1000.00");
        wallet.lockWallet();
        expectDomainException("khong duoc rut khi vi bi khoa",
                () -> wallet.withdrawMoney(new BigDecimal("1.00")));
        check("so du giu nguyen khi vi bi khoa", Money.of("1000.00"), wallet.balance());
    }

    private static void khongDuocRutSoTienKhongHopLe() {
        Wallet wallet = activeWallet("1000.00");
        // Chan so am KHONG phai luat them vao: neu bo, subtract(-100) se cong
        // tien vao vi. Rang buoc nay nam san trong Value Object Money.
        expectDomainException("khong duoc rut so am",
                () -> wallet.withdrawMoney(new BigDecimal("-100.00")));
        expectNullPointer("khong duoc rut so tien null",
                () -> wallet.withdrawMoney(null));
    }

    /**
     * Chay CUNG MOT kich ban qua ban cu va ban moi.
     *
     * Ban cu: rut 5000 tu vi co 1000 -> khong loi, so du thanh -4000.
     * Ban moi: cung thao tac -> DomainException, so du giu nguyen 1000.
     */
    private static void luatBatBienChanDuocLoiCuaBanCu() {
        LegacyWalletCode.WalletEntity legacy = new LegacyWalletCode.WalletEntity();
        legacy.id = UUID.randomUUID();
        legacy.balance = new BigDecimal("1000.00");
        legacy.status = "ACTIVE";
        try {
            new LegacyWalletCode.WalletService().withdraw(legacy, new BigDecimal("5000.00"));
        } catch (Exception e) {
            fail("ban cu le ra khong chan gi ca nhung lai nem: " + e.getMessage());
        }
        check("ban cu cho so du tut xuong am", new BigDecimal("-4000.00"), legacy.balance);

        Wallet wallet = activeWallet("1000.00");
        expectDomainException("ban moi chan dung kich ban do",
                () -> wallet.withdrawMoney(new BigDecimal("5000.00")));
        check("ban moi giu so du nguyen ven", Money.of("1000.00"), wallet.balance());
    }

    // --- TANG APPLICATION ---------------------------------------------------

    private static void useCaseChiDieuPhoiVaTraDto() {
        WalletRepository repository = new InMemoryWalletRepository();
        WalletId walletId = WalletId.random();
        repository.save(Wallet.open(walletId, Money.of("500.00")));

        WithdrawMoneyUseCase withdrawMoney = new WithdrawMoneyService(repository);
        WalletSnapshot snapshot = withdrawMoney.withdrawMoney(
                new WithdrawMoneyCommand(walletId.value(), new BigDecimal("200.00")));

        check("use case tra ve DTO chu khong phai aggregate",
                new BigDecimal("300.00"), snapshot.balance());
        check("trang thai trong DTO la chuoi", "ACTIVE", snapshot.status());
        check("thay doi da duoc ghi xuong kho", Money.of("300.00"),
                repository.findById(walletId).orElseThrow().balance());

        LockWalletUseCase lockWallet = new LockWalletService(repository);
        check("khoa vi qua use case", "LOCKED", lockWallet.lockWallet(walletId.value()).status());
    }

    private static void khongTimThayViLaLoiUngDungChuKhongPhaiLoiNghiepVu() {
        WithdrawMoneyUseCase withdrawMoney = new WithdrawMoneyService(new InMemoryWalletRepository());
        try {
            withdrawMoney.withdrawMoney(
                    new WithdrawMoneyCommand(UUID.randomUUID(), new BigDecimal("10.00")));
            fail("le ra phai nem WalletNotFoundException");
        } catch (WalletNotFoundException e) {
            pass("khong tim thay vi -> WalletNotFoundException, khong phai DomainException");
        }
    }

    /** Thao tac bi tu choi thi khong duoc de lai dau vet nao trong kho. */
    private static void aggregateBiTuChoiThiKhongGhiXuongKho() {
        WalletRepository repository = new InMemoryWalletRepository();
        WalletId walletId = WalletId.random();
        repository.save(Wallet.open(walletId, Money.of("100.00")));

        WithdrawMoneyUseCase withdrawMoney = new WithdrawMoneyService(repository);
        expectDomainException("use case khong nuot DomainException cua aggregate",
                () -> withdrawMoney.withdrawMoney(
                        new WithdrawMoneyCommand(walletId.value(), new BigDecimal("999.00"))));

        check("kho giu nguyen so du cu", Money.of("100.00"),
                repository.findById(walletId).orElseThrow().balance());
    }

    // --- HA TANG TEST -------------------------------------------------------

    private static Wallet activeWallet(String balance) {
        return Wallet.open(WalletId.random(), Money.of(balance));
    }

    private static void expectDomainException(String name, Runnable action) {
        try {
            action.run();
            fail(name + " - le ra phai nem DomainException");
        } catch (DomainException e) {
            pass(name);
        }
    }

    private static void expectNullPointer(String name, Runnable action) {
        try {
            action.run();
            fail(name + " - le ra phai nem NullPointerException");
        } catch (NullPointerException e) {
            pass(name);
        }
    }

    private static void check(String name, Object expected, Object actual) {
        if (expected.equals(actual)) {
            pass(name);
        } else {
            fail(name + " - mong doi <" + expected + "> nhung nhan <" + actual + ">");
        }
    }

    static void pass(String name) {
        passed++;
        System.out.println("  [OK]   " + name);
    }

    static void fail(String message) {
        failed++;
        System.out.println("  [LOI]  " + message);
    }

    private SelfCheck() {
    }
}
