package com.example.logistics.infrastructure.carrier.ghtk;

import com.example.logistics.core.domain.CarrierCode;
import com.example.logistics.core.domain.Money;
import com.example.logistics.core.domain.ShipmentRequest;
import com.example.logistics.core.domain.ShippingQuote;
import com.example.logistics.core.port.out.CarrierUnavailableException;
import com.example.logistics.core.port.out.ShippingCarrierPort;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ADAPTER GHTK - yeu cau c.
 *
 * Cung mot Port ShippingCarrierPort nhung ben duoi la mot the gioi khac han
 * GHN. Toan bo phan dich thuat nam gon trong file nay:
 *
 *   gram   -> kilogram        (don vi cua doi tac)
 *   JSON   -> so              (boc tach payload tho)
 *   nghin dong -> dong        (don vi tien)
 *   gio    -> ngay (lam tron len)
 *   GhtkApiException -> CarrierUnavailableException
 *
 * Use Case khong biet mot chu nao trong nhung dieu tren - no chi nhan ve
 * ShippingQuote giong het nhu khi goi GHN.
 */
public final class GhtkCarrierAdapter implements ShippingCarrierPort {

    private static final int HOURS_PER_DAY = 24;

    private final GhtkRestClient client;

    public GhtkCarrierAdapter(GhtkRestClient client) {
        this.client = Objects.requireNonNull(client, "GHTK client must not be null");
    }

    public GhtkCarrierAdapter(String baseUrl) {
        this(new GhtkRestClient(baseUrl));
    }

    @Override
    public CarrierCode carrier() {
        return CarrierCode.GHTK;
    }

    @Override
    public ShippingQuote calculateFee(ShipmentRequest request) {
        try {
            String payload = client.getShipmentFee(
                    request.getDestination().key(),
                    request.getWeight().toKilograms());

            long feeThousand = readNumber(payload, "fee");
            long deliveryHours = readNumber(payload, "delivery_time");
            int estimatedDays = (int) Math.max(1,
                    (deliveryHours + HOURS_PER_DAY - 1) / HOURS_PER_DAY);

            return new ShippingQuote(
                    carrier(),
                    Money.ofVnd(feeThousand * 1_000),
                    estimatedDays,
                    "GHTK Tiet Kiem (" + deliveryHours + "h)");
        } catch (GhtkApiException e) {
            throw new CarrierUnavailableException(carrier(),
                    "GHTK tu choi bao gia: " + e.getMessage(), e);
        }
    }

    /**
     * Boc mot truong so ra khoi JSON tho.
     *
     * Du an that se dung Jackson/Gson; o day dung bieu thuc chinh quy de bai
     * tap khong can thu vien ngoai. Diem quan trong ve kien truc van giu nguyen:
     * viec doc JSON la chi tiet cua Infrastructure, Core khong he hay biet.
     */
    private static long readNumber(String json, String field) {
        Matcher matcher = Pattern.compile("\"" + field + "\"\\s*:\\s*(-?\\d+)").matcher(json);
        if (!matcher.find()) {
            throw new CarrierUnavailableException(CarrierCode.GHTK,
                    "Phan hoi GHTK thieu truong '" + field + "': " + json);
        }
        return Long.parseLong(matcher.group(1));
    }
}
