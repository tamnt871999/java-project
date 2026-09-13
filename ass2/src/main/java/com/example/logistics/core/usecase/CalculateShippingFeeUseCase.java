package com.example.logistics.core.usecase;

import com.example.logistics.core.domain.ShipmentRequest;
import com.example.logistics.core.domain.ShippingQuote;
import com.example.logistics.core.factory.ShippingCarrierFactory;
import com.example.logistics.core.factory.ShippingCarrierProvider;
import com.example.logistics.core.port.in.CalculateShippingFeePort;
import com.example.logistics.core.port.out.CarrierUnavailableException;
import com.example.logistics.core.port.out.ShippingCarrierPort;

import java.util.Objects;

/**
 * USE CASE - yeu cau b.
 *
 * Dieu phoi mot luong nghiep vu duy nhat: nhan yeu cau giao hang -> chon nha
 * van chuyen -> lay bao gia -> tra ve.
 *
 * KIEM TRA DO SACH CUA FILE NAY: nhin phan import o tren, khong co mot dong
 * nao tro toi package infrastructure, khong co HttpClient, khong co SDK, khong
 * co ten lop GhnCarrierAdapter / GhtkCarrierAdapter. Use Case chi biet den
 * Domain, Port va Factory - deu la tai san cua chinh Core.
 *
 * Cung vi vay o day KHONG co doan:
 *
 *     if (city.equals("Ha Noi")) { new GhnSdk().fee(...); }
 *     else                       { new GhtkClient().fee(...); }
 *
 * Doan if-else do vi pham OCP: moi lan ky hop dong voi mot doi tac moi lai
 * phai mo Use Case ra sua, va moi lan sua la mot lan co the lam hong luong
 * dat hang dang chay on dinh. O kien truc nay, viec chon doi tac da duoc day
 * ra ShippingCarrierProvider, con cach goi doi tac da duoc day ra Adapter.
 */
public class CalculateShippingFeeUseCase implements CalculateShippingFeePort {

    private final ShippingCarrierProvider carrierProvider;

    /**
     * CONSTRUCTOR INJECTION - phu thuoc duoc tiem tu ben ngoai vao, Use Case
     * khong tu tao lay. Truong final + kiem tra null tai day dam bao doi tuong
     * da dung xong la dung duoc ngay, khong ton tai trang thai nua voi.
     *
     * Kieu tham so la INTERFACE ShippingCarrierProvider chu khong phai class
     * ShippingCarrierFactory: doi chien luoc chon hang (re nhat, failover, ...)
     * thi file nay khong phai sua mot chu nao.
     */
    public CalculateShippingFeeUseCase(ShippingCarrierProvider carrierProvider) {
        this.carrierProvider = Objects.requireNonNull(carrierProvider, "carrierProvider must not be null");
    }

    /**
     * Dang tiem thang danh sach Port (dung cho kiem thu hoac khi chi co mot vai
     * doi tac): cac adapter duoc truyen vao qua constructor, Factory mac dinh
     * duoc dung ben trong.
     */
    public CalculateShippingFeeUseCase(ShippingCarrierPort... carriers) {
        this(ShippingCarrierFactory.withDefaultPolicy(carriers));
    }

    @Override
    public ShippingQuote calculate(ShipmentRequest request) {
        Objects.requireNonNull(request, "request must not be null");

        // 1. Chon chien luoc van chuyen cho yeu cau nay (khong biet ben duoi la ai).
        ShippingCarrierPort carrier = carrierProvider.carrierFor(request);

        // 2. Goi qua Port. Neu doi tac loi, Adapter da dich san sang
        //    CarrierUnavailableException nen o day khong phai bat kieu la nao.
        ShippingQuote quote = carrier.calculateFee(request);

        // 3. Kiem tra hop dong: adapter phai tra ket qua, va phai khai dung ten
        //    minh. Day la rao chan chong mot adapter viet au lam ban du lieu loi.
        if (quote == null) {
            throw new CarrierUnavailableException(carrier.carrier(),
                    "Nha van chuyen " + carrier.carrier() + " khong tra ve bao gia");
        }
        if (!quote.getCarrier().equals(carrier.carrier())) {
            throw new CarrierUnavailableException(carrier.carrier(),
                    "Bao gia tra ve sai nha van chuyen: mong doi " + carrier.carrier()
                            + " nhung nhan duoc " + quote.getCarrier());
        }
        return quote;
    }
}
