package com.example.wallet;

import com.example.wallet.domain.DomainException;
import com.example.wallet.domain.Money;
import com.example.wallet.domain.Wallet;
import com.example.wallet.domain.WalletId;
import com.example.wallet.domain.WalletStatus;
import com.example.wallet.legacy.LegacyWalletCode;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Wallet - rich domain model tu canh invariant")
class WalletTest {

    private static Wallet viCoSoDu(String soDu) {
        return Wallet.open(WalletId.random(), Money.of(soDu));
    }

    @Test
    @DisplayName("vi moi mo luon o trang thai ACTIVE")
    void viMoiMoLuonActive() {
        Wallet wallet = viCoSoDu("1000.00");

        assertThat(wallet.status()).isEqualTo(WalletStatus.ACTIVE);
        assertThat(wallet.balance()).isEqualTo(Money.of("1000.00"));
    }

    @Test
    @DisplayName("rut tien hop le thi so du giam dung")
    void rutTienHopLe() {
        Wallet wallet = viCoSoDu("1000.00");

        wallet.withdrawMoney(new BigDecimal("300.00"));

        assertThat(wallet.balance()).isEqualTo(Money.of("700.00"));
    }

    @Test
    @DisplayName("khoa vi la idempotent - khoa hai lan khong bao loi")
    void khoaViHaiLanKhongBaoLoi() {
        Wallet wallet = viCoSoDu("1000.00");

        wallet.lockWallet();
        wallet.lockWallet();

        assertThat(wallet.status()).isEqualTo(WalletStatus.LOCKED);
    }

    @Nested
    @DisplayName("Ba invariant khong the lach")
    class Invariant {

        @Test
        @DisplayName("vi bi khoa thi khong rut duoc")
        void viBiKhoaKhongRutDuoc() {
            Wallet wallet = viCoSoDu("1000.00");
            wallet.lockWallet();

            assertThatThrownBy(() -> wallet.withdrawMoney(new BigDecimal("100.00")))
                    .isInstanceOf(DomainException.class)
                    .hasMessageContaining("bi khoa");

            assertThat(wallet.balance()).isEqualTo(Money.of("1000.00"));
        }

        @Test
        @DisplayName("khong rut qua so du - chinh la luat ma ban cu bo quen")
        void khongRutQuaSoDu() {
            Wallet wallet = viCoSoDu("1000.00");

            assertThatThrownBy(() -> wallet.withdrawMoney(new BigDecimal("5000.00")))
                    .isInstanceOf(DomainException.class)
                    .hasMessageContaining("So du khong du");

            assertThat(wallet.balance()).isEqualTo(Money.of("1000.00"));
        }

        @Test
        @DisplayName("rut so am bi Money chan - neu khong se thanh cong tien vao vi")
        void rutSoAmBiChan() {
            Wallet wallet = viCoSoDu("1000.00");

            assertThatThrownBy(() -> wallet.withdrawMoney(new BigDecimal("-100.00")))
                    .isInstanceOf(DomainException.class);

            assertThat(wallet.balance()).isEqualTo(Money.of("1000.00"));
        }
    }

    @Nested
    @DisplayName("Doi chieu voi ban anemic cu")
    class DoiChieuBanCu {

        @Test
        @DisplayName("ban cu CO BUG: rut qua so du van chay, vi tut xuong am")
        void banCuChoPhepSoDuAm() throws Exception {
            LegacyWalletCode.WalletEntity cu = new LegacyWalletCode.WalletEntity();
            cu.id = UUID.randomUUID();
            cu.balance = new BigDecimal("1000.00");
            cu.status = "ACTIVE";

            new LegacyWalletCode.WalletService().withdraw(cu, new BigDecimal("5000.00"));

            assertThat(cu.balance).isEqualByComparingTo(new BigDecimal("-4000.00"));
            assertThat(cu.balance.signum()).isNegative();
        }

        @Test
        @DisplayName("ban cu CO BUG: gan status sai chinh ta la khoa mat tac dung")
        void banCuKhoaMatTacDungKhiSaiChinhTa() throws Exception {
            LegacyWalletCode.WalletEntity cu = new LegacyWalletCode.WalletEntity();
            cu.id = UUID.randomUUID();
            cu.balance = new BigDecimal("1000.00");
            cu.status = "Locked";

            new LegacyWalletCode.WalletService().withdraw(cu, new BigDecimal("100.00"));

            assertThat(cu.balance).isEqualByComparingTo(new BigDecimal("900.00"));
        }

        @Test
        @DisplayName("ban moi: khong con mot setter cong khai nao tren Wallet")
        void banMoiKhongConSetter() {
            for (Method method : Wallet.class.getDeclaredMethods()) {
                assertThat(method.getName())
                        .as("Wallet khong duoc co setter cong khai")
                        .doesNotStartWith("set");
            }
        }

        @Test
        @DisplayName("ban moi: balance va status khong the sua tu ben ngoai")
        void banMoiKhongSuaDuocTruongTuBenNgoai() {
            assertThat(Wallet.class.getDeclaredFields())
                    .allSatisfy(field -> assertThat(field.getModifiers() & java.lang.reflect.Modifier.PRIVATE)
                            .as("truong %s phai private", field.getName())
                            .isNotZero());
        }
    }
}
