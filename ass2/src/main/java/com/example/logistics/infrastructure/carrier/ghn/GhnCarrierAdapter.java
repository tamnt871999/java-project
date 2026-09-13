package com.example.logistics.infrastructure.carrier.ghn;

import com.example.logistics.core.domain.CarrierCode;
import com.example.logistics.core.domain.Money;
import com.example.logistics.core.domain.ShipmentRequest;
import com.example.logistics.core.domain.ShippingQuote;
import com.example.logistics.core.port.out.CarrierUnavailableException;
import com.example.logistics.core.port.out.ShippingCarrierPort;

import java.util.Objects;

/**
 * ADAPTER GHN - yeu cau c.
 *
 * Nam o tang Infrastructure va phu thuoc VAO Core (implement ShippingCarrierPort).
 * Chieu phu thuoc la ngoai -> trong, dung nguyen tac cua Hexagonal.
 *
 * Toan bo cong viec ban thiu deu dung lai o file nay:
 *   - dung FeeRequest theo dung kieu cua SDK,
 *   - doi Weight (gram trong Domain) sang int gram cua SDK,
 *   - kiem tra ma loi so (code != 200) do SDK tra ve,
 *   - doi int total cua SDK sang Money cua Domain,
 *   - bat GhnApiException va dich sang CarrierUnavailableException.
 *
 * Neu mai mot GHN doi SDK, chi mot minh file nay phai sua.
 */
public final class GhnCarrierAdapter implements ShippingCarrierPort {

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
        try {
            GhnShippingSdk.FeeRequest sdkRequest = new GhnShippingSdk.FeeRequest();
            // SDK so khop ten dia danh theo chuoi thuong khong dau -> dung City.key().
            sdkRequest.toDistrictName = request.getDestination().key();
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
}
