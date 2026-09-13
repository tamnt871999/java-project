package com.example.logistics.infrastructure.cli;

import com.example.logistics.core.domain.CarrierCode;
import com.example.logistics.core.domain.DomainException;
import com.example.logistics.core.domain.ShipmentRequest;
import com.example.logistics.core.domain.ShippingQuote;
import com.example.logistics.core.factory.CarrierRoutingPolicy;
import com.example.logistics.core.port.in.CalculateShippingFeePort;
import com.example.logistics.core.port.out.CarrierUnavailableException;

import java.io.PrintStream;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * DRIVING ADAPTER (inbound adapter) - phia chu dong goi vao loi.
 *
 * Hexagonal co hai loai adapter, de lan nhau:
 *
 *   DRIVING (nay)  : nguoi dung / he thong khac goi VAO loi.
 *                    CLI nay, va sau nay la REST controller, message consumer.
 *                    No cam vao INBOUND PORT (CalculateShippingFeePort).
 *
 *   DRIVEN         : loi goi RA the gioi ben ngoai.
 *                    GhnCarrierAdapter, GhtkCarrierAdapter o package carrier.
 *                    Chung hien thuc OUTBOUND PORT (ShippingCarrierPort).
 *
 * Tat ca viec trinh bay deu dung lai o day: dinh dang tien te theo vung mien,
 * ke bang, doi chieu do rong cot, dich ngoai le thanh cau thong bao cho nguoi
 * doc. Loi khong biet gi ve nhung thu do - no chi tra ve ShippingQuote.
 *
 * PrintStream duoc tiem vao chu khong goi thang System.out, de bai kiem thu
 * bat duoc dau ra ma khong phai doi huong luong chuan cua ca tien trinh.
 */
public final class ShippingFeeCliAdapter {

    private final CalculateShippingFeePort calculateShippingFee;
    private final CarrierRoutingPolicy routingPolicy;
    private final Set<CarrierCode> registeredCarriers;
    private final PrintStream out;

    public ShippingFeeCliAdapter(CalculateShippingFeePort calculateShippingFee,
                                 CarrierRoutingPolicy routingPolicy,
                                 Set<CarrierCode> registeredCarriers) {
        this(calculateShippingFee, routingPolicy, registeredCarriers, System.out);
    }

    public ShippingFeeCliAdapter(CalculateShippingFeePort calculateShippingFee,
                                 CarrierRoutingPolicy routingPolicy,
                                 Set<CarrierCode> registeredCarriers,
                                 PrintStream out) {
        this.calculateShippingFee = Objects.requireNonNull(calculateShippingFee,
                "calculateShippingFee must not be null");
        this.routingPolicy = Objects.requireNonNull(routingPolicy, "routingPolicy must not be null");
        this.registeredCarriers = Set.copyOf(
                Objects.requireNonNull(registeredCarriers, "registeredCarriers must not be null"));
        this.out = Objects.requireNonNull(out, "out must not be null");
    }

    /**
     * Diem vao cua adapter.
     *
     *   (khong tham so)      -> chay bo kich ban demo
     *   "Ha Noi" 1200        -> bao gia mot don
     */
    public void run(String[] args) {
        if (args.length >= 2) {
            quoteOnce(args[0], args[1]);
        } else {
            runDemo();
        }
    }

    private void quoteOnce(String city, String grams) {
        try {
            ShipmentRequest request = ShipmentRequest.of(city, Long.parseLong(grams.trim()));
            ShippingQuote quote = calculateShippingFee.calculate(request);
            out.println();
            out.println("  Diem den    : " + request.getDestination().displayName()
                    + "  (khoa chuan hoa: " + request.getDestination().key() + ")");
            out.println("  Trong luong : " + request.getWeight());
            out.println("  Nha van chuyen: " + quote.getCarrier().partnerName()
                    + " [" + quote.getCarrier() + "]");
            out.println("  Cuoc phi    : " + money(quote) + " d");
            out.println("  Du kien     : " + quote.getEstimatedDays() + " ngay");
            out.println("  Ghi chu     : " + quote.getNote());
            out.println();
        } catch (NumberFormatException e) {
            out.println("Trong luong phai la so gram nguyen, vi du: 1200");
        } catch (DomainException | CarrierUnavailableException e) {
            out.println("Khong bao gia duoc: " + e.getMessage());
        }
    }

    private void runDemo() {
        out.println();
        out.println("=== BAI TAP 2 - PORTS & ADAPTERS CHO LOGISTICS ===");
        out.println();

        out.println("Adapter da dang ky : " + registeredCarriers);
        out.println("Luat dinh tuyen    :");
        for (Map.Entry<String, CarrierCode> route : routingPolicy.routes().entrySet()) {
            out.printf("    %-16s -> %s%n", route.getKey(), route.getValue());
        }
        out.printf("    %-16s -> %s%n", "(con lai)", routingPolicy.fallback());
        out.println();

        out.printf("%-22s %8s   %-22s %10s %7s   %s%n",
                "DIEM DEN", "K.LUONG", "NHA VAN CHUYEN", "CUOC PHI", "NGAY", "GHI CHU");
        out.println("-".repeat(100));

        // Cung mot doan code goi Use Case cho MOI tinh huong - khong he co
        // if-else chon doi tac o day.
        quoteRow("Ha Noi", 800);
        quoteRow("TP. Ho Chi Minh", 1200);
        quoteRow("hcm", 1200);                // bi danh -> van ra GHN
        // Ten co dau: file nguon luu UTF-8 va javac chay kem -encoding UTF-8
        // (xem run.ps1). City tu boc dau truoc khi dinh tuyen.
        quoteRow("Đà Nẵng", 2500);            // co dau -> van ra GHN
        quoteRow("Nghe An", 800);             // tinh le -> GHTK
        quoteRow("Ca Mau", 3000);             // tinh le -> GHTK
        quoteRow("Binh Duong", 5000);         // tinh le nhung gan -> GHTK re
        quoteRow("Son La", 25000);            // 25kg -> GHTK tu choi

        out.println();
        out.println("Nhan xet: khi diem den doi tu thanh pho lon sang tinh le, nha van");
        out.println("chuyen tu dong doi tu GHN sang GHTK. Use Case khong co dong if-else");
        out.println("nao ve ten doi tac - viec chon do ShippingCarrierProvider dam nhiem.");
        out.println();
    }

    private void quoteRow(String city, long grams) {
        ShipmentRequest request = ShipmentRequest.of(city, grams);
        try {
            ShippingQuote quote = calculateShippingFee.calculate(request);
            out.printf("%-22s %8s   %-22s %10s %7d   %s%n",
                    request.getDestination().displayName(),
                    request.getWeight(),
                    quote.getCarrier().partnerName(),
                    money(quote),
                    quote.getEstimatedDays(),
                    quote.getNote());
        } catch (CarrierUnavailableException e) {
            out.printf("%-22s %8s   %-22s %s%n",
                    request.getDestination().displayName(),
                    request.getWeight(),
                    e.getCarrier().value(),
                    "TU CHOI - " + e.getMessage());
        }
    }

    /** Dinh dang tien de HIEN THI - viec cua adapter, khong phai cua Domain. */
    private static String money(ShippingQuote quote) {
        return String.format(Locale.US, "%,d", quote.getFee().toVnd());
    }
}
