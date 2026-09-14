package com.example.wallet.legacy;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * MA NGUON TOI BAN DAU cua de bai, giu nguyen de doi chieu.
 *
 * Co y dat trong src/test/java chu khong phai src/main/java: day la tang vat
 * dung de CHUNG MINH loi cu, khong phai mot phan cua he thong. De o main thi
 * no se lot vao ban dong goi, va fitness function kien truc cung se bao do.
 *
 * Bai test luatBatBienChanDuocLoiCuaBanCu() trong SelfCheck chay cung mot kich
 * ban qua ban cu va ban moi de thay khac biet.
 */
public final class LegacyWalletCode {

    /** Nguyen van WalletEntity cua de bai: moi thuoc tinh deu public. */
    public static class WalletEntity {
        public UUID id;
        public BigDecimal balance;
        public String status; // "ACTIVE", "LOCKED"
    }

    /** Nguyen van WalletService cua de bai, ke ca loi bo quen kiem tra so du. */
    public static class WalletService {
        public void withdraw(WalletEntity wallet, BigDecimal amount) throws Exception {
            if ("LOCKED".equals(wallet.status)) {
                throw new Exception("Vi dien tu hien dang bi khoa!");
            }
            // LOI CHI MANG: thieu kiem tra dieu kien so du, cho phep balance bi
            // tru am tu do.
            wallet.balance = wallet.balance.subtract(amount);
        }
    }

    private LegacyWalletCode() {
    }
}
