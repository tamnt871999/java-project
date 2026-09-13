package com.example.logistics.infrastructure.carrier.ghtk;

import com.example.logistics.core.domain.CarrierCode;
import com.example.logistics.core.domain.City;
import com.example.logistics.core.domain.Money;
import com.example.logistics.core.port.dto.ShipmentRequest;
import com.example.logistics.core.port.dto.ShippingQuote;
import com.example.logistics.core.port.out.CarrierUnavailableException;
import com.example.logistics.core.port.out.ShippingCarrierPort;

import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ADAPTER GHTK - yeu cau c.
 *
 * Cung mot Port ShippingCarrierPort nhung ben duoi la mot the gioi khac han
 * GHN. Toan bo phan dich thuat nam gon trong file nay:
 *
 *   khoa chuan hoa -> ten tinh co dau  (dinh danh dia ban cua doi tac)
 *   gram           -> kilogram        (don vi cua doi tac)
 *   JSON           -> so / chuoi      (boc tach payload tho)
 *   nghin dong     -> dong            (don vi tien)
 *   gio            -> ngay (lam tron len)
 *   GhtkApiException -> CarrierUnavailableException
 *
 * Use Case khong biet mot chu nao trong nhung dieu tren - no chi nhan ve
 * ShippingQuote giong het nhu khi goi GHN.
 */
public final class GhtkCarrierAdapter implements ShippingCarrierPort {

    private static final int HOURS_PER_DAY = 24;

    /**
     * BANG ANH XA DIA CHI - tai san rieng cua adapter nay.
     *
     * Doi chieu voi GhnCarrierAdapter.DISTRICT_IDS de thay ro van de: cung mot
     * tinh, GHN dinh danh bang so 1854, con GHTK doi ten tieng Viet co dau
     * "Nghe An" (co dau). Hai bang hoan toan khac nhau, va deu la chi tiet
     * rieng cua doi tac.
     *
     * Domain chi giu mot dang duy nhat - khoa chuan hoa khong dau - roi moi
     * adapter tu dich sang dinh dang ma doi tac cua no doi hoi. Nho vay ky them
     * doi tac thu ba khong lam phinh City them mot truong nao.
     *
     * Du an that nap bang nay tu API danh muc cua GHTK hoac tu file cau hinh,
     * khong hard-code. O day liet ke san cho gon.
     */
    private static final Map<String, String> PROVINCE_NAMES = Map.ofEntries(
            Map.entry("ha noi", "Hà Nội"),
            Map.entry("ho chi minh", "Hồ Chí Minh"),
            Map.entry("da nang", "Đà Nẵng"),
            Map.entry("hai phong", "Hải Phòng"),
            Map.entry("can tho", "Cần Thơ"),
            Map.entry("binh duong", "Bình Dương"),
            Map.entry("dong nai", "Đồng Nai"),
            Map.entry("long an", "Long An"),
            Map.entry("bac ninh", "Bắc Ninh"),
            Map.entry("hung yen", "Hưng Yên"),
            Map.entry("hai duong", "Hải Dương"),
            Map.entry("quang ninh", "Quảng Ninh"),
            Map.entry("thanh hoa", "Thanh Hóa"),
            Map.entry("nghe an", "Nghệ An"),
            Map.entry("thua thien hue", "Thừa Thiên Huế"),
            Map.entry("khanh hoa", "Khánh Hòa"),
            Map.entry("lam dong", "Lâm Đồng"),
            Map.entry("an giang", "An Giang"),
            Map.entry("ca mau", "Cà Mau"),
            Map.entry("lao cai", "Lào Cai"),
            Map.entry("son la", "Sơn La"));

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
        String province = provinceNameOf(request.getDestination());
        try {
            String payload = client.getShipmentFee(province, request.getWeight().toKilograms());

            long feeThousand = readNumber(payload, "fee");
            long deliveryHours = readNumber(payload, "delivery_time");
            int estimatedDays = (int) Math.max(1,
                    (deliveryHours + HOURS_PER_DAY - 1) / HOURS_PER_DAY);

            return new ShippingQuote(
                    carrier(),
                    Money.ofVnd(feeThousand * 1_000),
                    estimatedDays,
                    readText(payload, "name"));
        } catch (GhtkApiException e) {
            throw new CarrierUnavailableException(carrier(),
                    "GHTK tu choi bao gia: " + e.getMessage(), e);
        }
    }

    /** Khoa chuan hoa cua Domain -> ten tinh co dau ma GHTK doi hoi. */
    private String provinceNameOf(City destination) {
        String province = PROVINCE_NAMES.get(destination.key());
        if (province == null) {
            throw new CarrierUnavailableException(carrier(),
                    "GHTK chua phuc vu dia ban: " + destination.displayName());
        }
        return province;
    }

    /**
     * Boc mot truong so ra khoi JSON tho.
     *
     * Du an that se dung Jackson/Gson; o day dung bieu thuc chinh quy de bai
     * tap khong can thu vien ngoai. Diem quan trong ve kien truc van giu nguyen:
     * viec doc JSON la chi tiet cua Infrastructure, Core khong he hay biet.
     */
    private static long readNumber(String json, String field) {
        return Long.parseLong(read(json, field, "(-?\\d+)"));
    }

    /** Boc mot truong chuoi ra khoi JSON tho. */
    private static String readText(String json, String field) {
        return read(json, field, "\"([^\"]*)\"");
    }

    private static String read(String json, String field, String valuePattern) {
        Matcher matcher = Pattern.compile("\"" + field + "\"\\s*:\\s*" + valuePattern).matcher(json);
        if (!matcher.find()) {
            throw new CarrierUnavailableException(CarrierCode.GHTK,
                    "Phan hoi GHTK thieu truong '" + field + "': " + json);
        }
        return matcher.group(1);
    }
}
