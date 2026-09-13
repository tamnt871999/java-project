package com.example.logistics.infrastructure.carrier.ghn;

import com.example.logistics.core.domain.CarrierCode;
import com.example.logistics.core.domain.City;
import com.example.logistics.core.domain.Money;
import com.example.logistics.core.port.dto.ShipmentRequest;
import com.example.logistics.core.port.dto.ShippingQuote;
import com.example.logistics.core.port.out.CarrierUnavailableException;
import com.example.logistics.core.port.out.ShippingCarrierPort;

import java.util.Map;
import java.util.Objects;

/**
 * ADAPTER GHN - yeu cau c.
 *
 * Nam o tang Infrastructure va phu thuoc VAO Core (implement ShippingCarrierPort).
 * Chieu phu thuoc la ngoai -> trong, dung nguyen tac cua Hexagonal.
 *
 * Toan bo cong viec ban thiu deu dung lai o file nay:
 *   - tra ten tinh / thanh cua Domain ra district_id trong danh muc cua GHN,
 *   - dung FeeRequest theo dung kieu cua SDK,
 *   - doi Weight (gram trong Domain) sang int gram cua SDK,
 *   - kiem tra ma loi so (code != 200) do SDK tra ve,
 *   - doi int total cua SDK sang Money cua Domain,
 *   - bat GhnApiException va dich sang CarrierUnavailableException.
 *
 * Neu mai mot GHN doi SDK hoac doi danh muc dia ban, chi mot minh file nay sua.
 */
public final class GhnCarrierAdapter implements ShippingCarrierPort {

    /**
     * BANG ANH XA DIA CHI - tai san rieng cua adapter nay.
     *
     * Vi sao bang nay PHAI nam o day chu khong nam trong Domain?
     *
     * "district_id = 1442" la thuat ngu rieng cua GHN, khong co y nghia voi bat
     * ky ai khac: GHTK dinh danh cung mot noi bang ten tieng Viet co dau, doi
     * tac thu ba lai co ma khac nua. Neu nhet bang nay vao City thi Domain phai
     * phinh ra them mot truong cho MOI doi tac ky hop dong - tuc la ky them doi
     * tac lai phai sua tang trong cung, dung dieu ma kien truc nay muon tranh.
     *
     * De o adapter thi moi doi tac tu lo phan dinh danh cua minh, va Domain chi
     * can biet mot thu duy nhat: ten tinh / thanh da chuan hoa.
     *
     * Du an that nap bang nay tu API danh muc cua GHN hoac tu file cau hinh,
     * khong hard-code. O day liet ke san cho gon.
     */
    private static final Map<String, Integer> DISTRICT_IDS = Map.ofEntries(
            Map.entry("ha noi", 1442),
            Map.entry("ho chi minh", 1454),
            Map.entry("da nang", 1526),
            Map.entry("hai phong", 1574),
            Map.entry("can tho", 1602),
            Map.entry("binh duong", 1630),
            Map.entry("dong nai", 1658),
            Map.entry("long an", 1686),
            Map.entry("bac ninh", 1714),
            Map.entry("hung yen", 1742),
            Map.entry("hai duong", 1770),
            Map.entry("quang ninh", 1798),
            Map.entry("thanh hoa", 1826),
            Map.entry("nghe an", 1854),
            Map.entry("thua thien hue", 1882),
            Map.entry("khanh hoa", 1910),
            Map.entry("lam dong", 1938),
            Map.entry("an giang", 1966),
            Map.entry("ca mau", 1994),
            Map.entry("lao cai", 2022),
            Map.entry("son la", 2050));

    private final GhnShippingSdk sdk;

    public GhnCarrierAdapter(GhnShippingSdk sdk) {
        this.sdk = Objects.requireNonNull(sdk, "GHN SDK must not be null");
    }

    /** Token la cau hinh ha tang - no chi duoc xuat hien o tang nay. */
    public GhnCarrierAdapter(String apiToken) {
        this(new GhnShippingSdk(apiToken));
    }

    @Override
    public CarrierCode carrier() {
        return CarrierCode.GHN;
    }

    @Override
    public ShippingQuote calculateFee(ShipmentRequest request) {
        int districtId = districtIdOf(request.getDestination());
        try {
            GhnShippingSdk.FeeRequest sdkRequest = new GhnShippingSdk.FeeRequest();
            sdkRequest.toDistrictId = districtId;
            sdkRequest.weightGram = (int) request.getWeight().grams();
            sdkRequest.serviceTypeId = GhnShippingSdk.SERVICE_TYPE_STANDARD;

            GhnShippingSdk.FeeResponse response = sdk.calculateFee(sdkRequest);
            if (response.code != 200) {
                throw new CarrierUnavailableException(carrier(),
                        "GHN tu choi bao gia (code " + response.code + "): " + response.message);
            }
            return new ShippingQuote(
                    carrier(),
                    Money.ofVnd(response.total),
                    response.expectedDeliveryDay,
                    response.serviceName);
        } catch (GhnApiException e) {
            // Dich ngoai le cua SDK sang ngon ngu cua loi - khong de ro ri ra ngoai.
            throw new CarrierUnavailableException(carrier(),
                    "Khong goi duoc GHN SDK: " + e.getMessage(), e);
        }
    }

    /** Ten tinh / thanh cua Domain -> district_id cua GHN. */
    private int districtIdOf(City destination) {
        Integer districtId = DISTRICT_IDS.get(destination.key());
        if (districtId == null) {
            throw new CarrierUnavailableException(carrier(),
                    "GHN chua phuc vu dia ban: " + destination.displayName());
        }
        return districtId;
    }
}
