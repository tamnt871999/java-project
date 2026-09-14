package com.example.wallet.domain;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * AGGREGATE ROOT - vong trong cung cua Clean Architecture, va la trong tam
 * cua bai tap nay.
 *
 * ================== TRUOC: ANEMIC MODEL ==================
 *
 *   public class WalletEntity {
 *       public UUID id;
 *       public BigDecimal balance;    // ai cung sua duoc
 *       public String status;         // ai cung sua duoc
 *   }
 *
 *   public class WalletService {
 *       public void withdraw(WalletEntity wallet, BigDecimal amount) throws Exception {
 *           if ("LOCKED".equals(wallet.status)) throw new Exception("...");
 *           wallet.balance = wallet.balance.subtract(amount);   // khong kiem tra so du
 *       }
 *   }
 *
 * WalletEntity o tren chi la mot TUI DU LIEU: no khong biet gi ve chinh no,
 * moi luat deu nam ben ngoai o WalletService. Hau qua khong phai ly thuyet:
 *
 *   - Luat "khong duoc rut qua so du" bi QUEN, va khong co gi nhac. Vi tut
 *     xuong am ma chuong trinh van chay binh thuong.
 *   - Du co sua WalletService cho dung, bat ky doan code nao khac trong he
 *     thong van co the viet "wallet.balance = ..." de di vong qua luat do.
 *     Sua mot cho khong lam he thong an toan.
 *
 * ================== SAU: RICH DOMAIN MODEL ==================
 *
 * Yeu cau a - dong goi:
 *   balance va status la private, khong co MOT setter cong khai nao. Cach duy
 *   nhat de so du thay doi la di qua withdrawMoney().
 *
 * Yeu cau b - phuong thuc giau hanh vi:
 *   withdrawMoney(BigDecimal) va lockWallet() dat ten theo NGHIEP VU chu khong
 *   theo ky thuat. So sanh "wallet.setBalance(x)" voi "wallet.withdrawMoney(x)":
 *   ten thu hai noi ro dieu gi dang xay ra va vi the co cho de gan luat vao.
 *
 * Yeu cau c - invariant:
 *   Hai luat bat bien duoc kiem NGAY TRONG aggregate, khong phai o service:
 *     - vi bi khoa thi khong rut duoc,
 *     - khong rut qua so du hien co.
 *   Vi chung nam trong aggregate, khong con duong nao lach duoc. Du co bao
 *   nhieu use case moi viet sau nay, vi cung khong the am.
 *
 * Aggregate nay KHA BIEN (mutable): withdrawMoney doi trang thai tai cho va
 * tra ve void, dung nhu chu ky ma de bai yeu cau. Aggregate kha bien la cach
 * lam kinh dien cua DDD - diem mau chot khong phai bat bien hay kha bien, ma
 * la moi thay doi deu phai di qua mot method co kiem tra luat.
 */
public class Wallet {

    private final WalletId id;
    private Money balance;
    private WalletStatus status;

    private Wallet(WalletId id, Money balance, WalletStatus status) {
        this.id = id;
        this.balance = balance;
        this.status = status;
    }

    /**
     * FACTORY METHOD: mo mot vi moi.
     *
     * Vi moi luon o trang thai ACTIVE - tang ngoai khong duoc quyen chon, vi
     * "mo vi trong trang thai bi khoa" la mot trang thai vo nghia.
     */
    public static Wallet open(WalletId id, Money initialBalance) {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(initialBalance, "initialBalance must not be null");
        return new Wallet(id, initialBalance, WalletStatus.ACTIVE);
    }

    /**
     * Dung lai aggregate tu du lieu da luu.
     *
     * Tach rieng khoi open() vi khoi phuc KHONG phai la mo vi moi: mot vi da
     * bi khoa tu hom qua phai song lai dung trang thai LOCKED, khong duoc
     * reset ve ACTIVE. Chi tang luu tru duoc goi ham nay.
     */
    public static Wallet rehydrate(WalletId id, Money balance, WalletStatus status) {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(balance, "balance must not be null");
        Objects.requireNonNull(status, "status must not be null");
        return new Wallet(id, balance, status);
    }

    /**
     * YEU CAU b + c - rut tien khoi vi.
     *
     * Nhan BigDecimal dung nhu de bai quy dinh; viec boc thanh Money la chuyen
     * rieng cua ben trong, tang ngoai khong phai biet Value Object cua domain
     * moi goi duoc ham nay.
     *
     * Thu tu kiem tra co chu y:
     *   1. So tien hop le chua? Day la loi CU PHAP cua lenh goi (so am, so 0),
     *      kiem truoc vi no khong lien quan gi den trang thai cua vi.
     *   2. Vi co dang bi khoa khong? Vi da khoa thi khoi ban den so du.
     *   3. So du co du khong? Chinh la luat ma ma nguon cu bo quen.
     *
     * @throws DomainException khi so tien khong hop le, vi dang bi khoa, hoac
     *         so du khong du
     */
    public void withdrawMoney(BigDecimal amount) {
        // Money tu chan null va so am - luat "tien khong the am" chi viet mot lan.
        Money requested = new Money(amount);
        if (requested.isZero()) {
            throw new DomainException("So tien rut phai lon hon 0");
        }
        if (status.isLocked()) {
            throw new DomainException("Vi dien tu hien dang bi khoa, khong the rut tien");
        }
        if (!balance.isAtLeast(requested)) {
            // Nem SO LIEU tho trong cau thong bao, khong dinh dang san theo vung
            // mien - viec do la cua tang ngoai.
            throw new DomainException("So du khong du: can " + requested
                    + " nhung chi con " + balance);
        }
        this.balance = balance.minus(requested);
    }

    /**
     * YEU CAU b - khoa vi.
     *
     * Co y KHONG lam idempotent: khoa mot vi da khoa la dau hieu logic goi
     * dang sai (goi hai lan, hoac hai luong cung xu ly mot su co), va aggregate
     * noi thang ra thay vi im lang nuot di.
     *
     * @throws DomainException neu vi da bi khoa tu truoc
     */
    public void lockWallet() {
        if (status.isLocked()) {
            throw new DomainException("Vi dien tu da bi khoa tu truoc");
        }
        this.status = WalletStatus.LOCKED;
    }

    public WalletId id() {
        return id;
    }

    /**
     * Chi doc. Money bat bien nen tra thang ra ngoai cung khong ai sua duoc -
     * day la ly do dung Value Object thay vi BigDecimal tran.
     */
    public Money balance() {
        return balance;
    }

    public WalletStatus status() {
        return status;
    }

    @Override
    public boolean equals(Object other) {
        // Entity so sanh theo DINH DANH, khong theo thuoc tinh: hai vi cung so
        // du van la hai vi khac nhau, va mot vi sau khi rut tien van la chinh no.
        return other instanceof Wallet wallet && id.equals(wallet.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Wallet[id=%s, balance=%s, status=%s]".formatted(id, balance, status);
    }
}
