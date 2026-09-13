package com.example.logistics;

import com.example.logistics.core.domain.CarrierCode;
import com.example.logistics.core.domain.City;
import com.example.logistics.core.domain.DomainException;
import com.example.logistics.core.domain.Money;
import com.example.logistics.core.factory.CarrierRoutingPolicy;
import com.example.logistics.core.factory.ShippingCarrierFactory;
import com.example.logistics.core.factory.ShippingCarrierProvider;
import com.example.logistics.core.port.dto.ShipmentRequest;
import com.example.logistics.core.port.dto.ShippingQuote;
import com.example.logistics.core.port.in.CalculateShippingFeePort;
import com.example.logistics.core.port.out.CarrierUnavailableException;
import com.example.logistics.core.port.out.ShippingCarrierPort;
import com.example.logistics.core.usecase.CalculateShippingFeeUseCase;
import com.example.logistics.infrastructure.carrier.ghn.GhnCarrierAdapter;
import com.example.logistics.infrastructure.carrier.ghtk.GhtkCarrierAdapter;

import java.util.List;

/**
 * Bo kiem thu toi gian, khong dung JUnit de bai tap chay duoc chi voi JDK.
 *
 *     .\run.ps1 test
 *
 * Diem dang chu y ve kien truc: cac bai test o duoi cam thang vao Use Case
 * bang mot adapter GIA (StubCarrierAdapter). Vi Use Case chi phu thuoc vao
 * Port chu khong phu thuoc vao SDK, ta kiem thu duoc nghiep vu ma khong can
 * mang, khong can token, khong can doi tac that.
 */
public final class SelfCheck {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        chuanHoaTenThanhPho();
        dinhTuyenTheoThanhPho();
        ghnTinhPhiTheoBlock500g();
        ghtkQuyDoiDonViVaThoiGian();
        ghtkTuChoiKienQuaNang();
        useCaseKhongPhuThuocDoiTacThat();
        themDoiTacMoiMaKhongSuaCodeCu();
        thieuAdapterThiBaoLoiCauHinh();
        tuChoiDuLieuDauVaoSai();
        doiChienLuocChonHangMaKhongSuaUseCase();
        adapterTuGiuBangAnhXaDiaChi();
        baoGiaLuonKemTenGoiDichVu();
        ArchitectureFitness.run();

        System.out.println();
        System.out.println("Ket qua: " + passed + " dat / " + failed + " loi");
        if (failed > 0) {
            System.exit(1);
        }
    }

    // ------------------------------------------------------------------ tests

    private static void chuanHoaTenThanhPho() {
        check("chuan hoa TP. Ho Chi Minh", "ho chi minh", City.of("TP. Ho Chi Minh").key());
        check("chuan hoa bi danh HCM", "ho chi minh", City.of("HCM").key());
        check("chuan hoa co dau Da Nang", "da nang", City.of("Đà Nẵng").key());
        check("chuan hoa Tinh Nghe An", "nghe an", City.of("Tinh Nghe An").key());
    }

    private static void dinhTuyenTheoThanhPho() {
        CalculateShippingFeePort useCase = defaultUseCase();
        check("Ha Noi -> GHN", CarrierCode.GHN,
                useCase.calculate(ShipmentRequest.of("Ha Noi", 800)).getCarrier());
        check("ho chi minh -> GHN", CarrierCode.GHN,
                useCase.calculate(ShipmentRequest.of("ho chi minh", 800)).getCarrier());
        check("Nghe An -> GHTK", CarrierCode.GHTK,
                useCase.calculate(ShipmentRequest.of("Nghe An", 800)).getCarrier());
        check("Ca Mau -> GHTK", CarrierCode.GHTK,
                useCase.calculate(ShipmentRequest.of("Ca Mau", 800)).getCarrier());
    }

    private static void ghnTinhPhiTheoBlock500g() {
        ShippingCarrierPort ghn = new GhnCarrierAdapter("TOKEN-TEST");
        // 22.000 co ban cho 500g dau, khong phu phi vi Ha Noi co hub.
        check("GHN 500g Ha Noi", Money.ofVnd(22_000),
                ghn.calculateFee(ShipmentRequest.of("Ha Noi", 500)).getFee());
        // 800g -> them 1 block 500g -> 27.000
        check("GHN 800g Ha Noi", Money.ofVnd(27_000),
                ghn.calculateFee(ShipmentRequest.of("Ha Noi", 800)).getFee());
        // Nghe An khong co hub -> cong 18.000 phu phi vung xa, 3 ngay.
        ShippingQuote xa = ghn.calculateFee(ShipmentRequest.of("Nghe An", 500));
        check("GHN 500g vung xa", Money.ofVnd(40_000), xa.getFee());
        check("GHN vung xa 3 ngay", 3, xa.getEstimatedDays());
    }

    private static void ghtkQuyDoiDonViVaThoiGian() {
        ShippingCarrierPort ghtk = new GhtkCarrierAdapter("https://demo.ghtk.vn");
        // 1kg noi vung: 16 nghin dong -> Adapter phai doi ra 16.000 dong.
        ShippingQuote gan = ghtk.calculateFee(ShipmentRequest.of("Ha Noi", 1000));
        check("GHTK doi nghin dong sang dong", Money.ofVnd(16_000), gan.getFee());
        // 36 gio -> lam tron len 2 ngay.
        check("GHTK doi 36h thanh 2 ngay", 2, gan.getEstimatedDays());
        // 3kg vung xa: 16 + 4 block nua kg * 4 + 10 phu phi = 42 nghin dong.
        ShippingQuote xa = ghtk.calculateFee(ShipmentRequest.of("Nghe An", 3000));
        check("GHTK 3kg vung xa", Money.ofVnd(42_000), xa.getFee());
        check("GHTK doi 72h thanh 3 ngay", 3, xa.getEstimatedDays());
    }

    private static void ghtkTuChoiKienQuaNang() {
        CalculateShippingFeePort useCase = defaultUseCase();
        try {
            useCase.calculate(ShipmentRequest.of("Son La", 25_000));
            fail("GHTK phai tu choi kien 25kg");
        } catch (CarrierUnavailableException e) {
            // Dieu quan trong: ngoai le nhan duoc la kieu cua CORE, khong phai
            // GhtkApiException cua ha tang -> chi tiet cong nghe khong ro ri.
            check("loi duoc dich sang kieu cua core", CarrierCode.GHTK, e.getCarrier());
        }
    }

    private static void useCaseKhongPhuThuocDoiTacThat() {
        // Adapter gia: khong SDK, khong mang. Use Case van chay binh thuong.
        ShippingCarrierPort stub = new StubCarrierAdapter(CarrierCode.GHN, 99_000);
        CalculateShippingFeePort useCase = new CalculateShippingFeeUseCase(stub);
        check("Use Case chay voi adapter gia", Money.ofVnd(99_000),
                useCase.calculate(ShipmentRequest.of("Ha Noi", 1000)).getFee());
    }

    private static void themDoiTacMoiMaKhongSuaCodeCu() {
        // Kich ban OCP: ky hop dong voi ViettelPost cho rieng Hue.
        // Khong mot file nao trong core / infrastructure phai sua.
        CarrierCode vtp = CarrierCode.of("VTP", "ViettelPost");
        CarrierRoutingPolicy policy = CarrierRoutingPolicy.builder()
                .route("Ha Noi", CarrierCode.GHN)
                .route("Hue", vtp)
                .fallback(CarrierCode.GHTK)
                .build();
        ShippingCarrierFactory factory = new ShippingCarrierFactory(policy, List.of(
                new GhnCarrierAdapter("TOKEN-TEST"),
                new GhtkCarrierAdapter("https://demo.ghtk.vn"),
                new StubCarrierAdapter(vtp, 31_000)));
        CalculateShippingFeePort useCase = new CalculateShippingFeeUseCase(factory);

        check("Hue -> VTP", vtp, useCase.calculate(ShipmentRequest.of("Hue", 1000)).getCarrier());
        check("Ha Noi van -> GHN", CarrierCode.GHN,
                useCase.calculate(ShipmentRequest.of("Ha Noi", 1000)).getCarrier());
        check("tinh khac van -> GHTK", CarrierCode.GHTK,
                useCase.calculate(ShipmentRequest.of("Ca Mau", 1000)).getCarrier());
    }

    private static void thieuAdapterThiBaoLoiCauHinh() {
        // Luat tro toi GHN nhung chi dang ky GHTK -> phai bao loi ro rang.
        ShippingCarrierFactory factory = new ShippingCarrierFactory(
                CarrierRoutingPolicy.defaultPolicy(),
                List.of(new GhtkCarrierAdapter("https://demo.ghtk.vn")));
        try {
            new CalculateShippingFeeUseCase(factory).calculate(ShipmentRequest.of("Ha Noi", 500));
            fail("phai bao thieu adapter GHN");
        } catch (CarrierUnavailableException e) {
            check("bao dung doi tac con thieu", CarrierCode.GHN, e.getCarrier());
        }
    }

    private static void tuChoiDuLieuDauVaoSai() {
        expectDomainError("trong luong 0", () -> ShipmentRequest.of("Ha Noi", 0));
        expectDomainError("trong luong am", () -> ShipmentRequest.of("Ha Noi", -5));
        expectDomainError("thanh pho rong", () -> ShipmentRequest.of("   ", 500));
        expectDomainError("khong khai bao fallback",
                () -> CarrierRoutingPolicy.builder().route("Ha Noi", CarrierCode.GHN).build());
    }

    /**
     * Loi ich thuc te cua viec Use Case phu thuoc INTERFACE ShippingCarrierProvider
     * thay vi class ShippingCarrierFactory.
     *
     * Kich ban: GHTK tu choi kien tren 20kg, nen di Son La 25kg thi bao gia that
     * bai (xem ghtkTuChoiKienQuaNang o tren). Kinh doanh yeu cau: kien qua nang
     * thi day sang GHN du tinh le co dat hon.
     *
     * Dap ung bang mot chien luoc chon hang MOI, khong sua Use Case, khong sua
     * Factory, khong sua Port, khong sua hai Adapter.
     */
    private static void doiChienLuocChonHangMaKhongSuaUseCase() {
        ShippingCarrierPort ghn = new GhnCarrierAdapter("TOKEN-TEST");
        ShippingCarrierPort ghtk = new GhtkCarrierAdapter("https://demo.ghtk.vn");
        ShippingCarrierFactory macDinh = new ShippingCarrierFactory(
                CarrierRoutingPolicy.defaultPolicy(), List.of(ghn, ghtk));

        CalculateShippingFeePort useCase =
                new CalculateShippingFeeUseCase(new HeavyParcelCarrierProvider(macDinh, ghn));

        // Kien nang: truoc day that bai, gio tu dong sang GHN.
        check("kien 25kg di Son La -> GHN", CarrierCode.GHN,
                useCase.calculate(ShipmentRequest.of("Son La", 25_000)).getCarrier());
        // Kien nhe: van theo luat dinh tuyen cu.
        check("kien 800g di Son La van -> GHTK", CarrierCode.GHTK,
                useCase.calculate(ShipmentRequest.of("Son La", 800)).getCarrier());
    }

    /**
     * Moi doi tac dinh danh dia ban mot kieu, va bang anh xa la tai san RIENG
     * cua tung Adapter chu khong phai cua Domain.
     *
     * Domain chi giu duy nhat khoa chuan hoa khong dau ("nghe an"); GHN dich
     * sang so 1854, GHTK dich sang "Nghe An" co dau. Dia ban ngoai danh muc thi
     * Adapter bao loi bang ngon ngu cua loi, khong nem kieu la ra ngoai.
     */
    private static void adapterTuGiuBangAnhXaDiaChi() {
        ShippingCarrierPort ghn = new GhnCarrierAdapter("TOKEN-TEST");
        ShippingCarrierPort ghtk = new GhtkCarrierAdapter("https://demo.ghtk.vn");

        // Cung mot City, hai adapter deu bao gia duoc du dinh danh khac han nhau.
        check("GHN nhan dien Nghe An", CarrierCode.GHN,
                ghn.calculateFee(ShipmentRequest.of("Nghe An", 500)).getCarrier());
        check("GHTK nhan dien Nghe An", CarrierCode.GHTK,
                ghtk.calculateFee(ShipmentRequest.of("Nghe An", 500)).getCarrier());

        // Dia ban chua co trong danh muc doi tac.
        expectCarrierUnavailable("GHN tu choi dia ban la", CarrierCode.GHN,
                () -> ghn.calculateFee(ShipmentRequest.of("Hoang Sa", 500)));
        expectCarrierUnavailable("GHTK tu choi dia ban la", CarrierCode.GHTK,
                () -> ghtk.calculateFee(ShipmentRequest.of("Hoang Sa", 500)));
    }

    /**
     * Bao gia phai kem ten GOI DICH VU - mot du kien nghiep vu that, khong phai
     * chuoi ghi chu tu do de Adapter nhet gi vao cung duoc.
     */
    private static void baoGiaLuonKemTenGoiDichVu() {
        ShippingCarrierPort ghn = new GhnCarrierAdapter("TOKEN-TEST");
        ShippingCarrierPort ghtk = new GhtkCarrierAdapter("https://demo.ghtk.vn");

        check("ten goi dich vu GHN", "GHN Standard",
                ghn.calculateFee(ShipmentRequest.of("Ha Noi", 500)).getServiceName());
        // Ten goi cua GHTK duoc boc ra tu payload JSON, khong phai chuoi ghep tay.
        check("ten goi dich vu GHTK", "GHTK Tiet Kiem",
                ghtk.calculateFee(ShipmentRequest.of("Ha Noi", 1000)).getServiceName());

        expectDomainError("bao gia thieu ten goi dich vu",
                () -> new ShippingQuote(CarrierCode.GHN, Money.ofVnd(1000), 1, "  "));
    }

    // ------------------------------------------------------------ ha tang test

    /**
     * Chien luoc chon hang thay the: kien tren 20kg thi dung hang chuyen kien
     * nang, con lai uy quyen cho chien luoc mac dinh.
     *
     * Day la bang chung cua diem thiet ke: Use Case chi thay ShippingCarrierProvider
     * nen khong he biet luat nay ton tai.
     */
    private static class HeavyParcelCarrierProvider implements ShippingCarrierProvider {

        private static final long NGUONG_NANG_GRAM = 20_000L;

        private final ShippingCarrierProvider macDinh;
        private final ShippingCarrierPort hangKienNang;

        HeavyParcelCarrierProvider(ShippingCarrierProvider macDinh, ShippingCarrierPort hangKienNang) {
            this.macDinh = macDinh;
            this.hangKienNang = hangKienNang;
        }

        @Override
        public ShippingCarrierPort carrierFor(ShipmentRequest request) {
            if (request.getWeight().grams() > NGUONG_NANG_GRAM) {
                return hangKienNang;
            }
            return macDinh.carrierFor(request);
        }
    }

    /** Adapter gia dung cho kiem thu - chung minh Port thay the duoc de dang. */
    private static class StubCarrierAdapter implements ShippingCarrierPort {

        private final CarrierCode carrierCode;
        private final long fixedFee;

        StubCarrierAdapter(CarrierCode carrierCode, long fixedFee) {
            this.carrierCode = carrierCode;
            this.fixedFee = fixedFee;
        }

        @Override
        public CarrierCode carrier() {
            return carrierCode;
        }

        @Override
        public ShippingQuote calculateFee(ShipmentRequest request) {
            return new ShippingQuote(carrierCode, Money.ofVnd(fixedFee), 2, "stub");
        }
    }

    private static CalculateShippingFeePort defaultUseCase() {
        return new CalculateShippingFeeUseCase(new ShippingCarrierFactory(
                CarrierRoutingPolicy.defaultPolicy(),
                List.of(new GhnCarrierAdapter("TOKEN-TEST"),
                        new GhtkCarrierAdapter("https://demo.ghtk.vn"))));
    }

    private static void expectCarrierUnavailable(String name, CarrierCode expected, Runnable action) {
        try {
            action.run();
            fail(name + " - le ra phai nem CarrierUnavailableException");
        } catch (CarrierUnavailableException e) {
            check(name, expected, e.getCarrier());
        }
    }

    private static void expectDomainError(String name, Runnable action) {
        try {
            action.run();
            fail(name + " - le ra phai nem DomainException");
        } catch (DomainException e) {
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
