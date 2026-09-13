package com.example.logistics.core.port.out;

import com.example.logistics.core.domain.CarrierCode;
import com.example.logistics.core.domain.ShipmentRequest;
import com.example.logistics.core.domain.ShippingQuote;

/**
 * OUTBOUND PORT (driven port) - yeu cau a.
 *
 * Day la HOP DONG do LOI dat ra cho the gioi ben ngoai: "ai muon lam nha van
 * chuyen cho toi thi phai tra loi duoc cau hoi: kien hang nang ngan nay, di
 * thanh pho nay, het bao nhieu tien va may ngay?".
 *
 * Ba diem quan trong ve mat kien truc:
 *
 *   1. Interface nam trong tang Application Core, KHONG nam trong Infrastructure.
 *      Day chinh la Dependency Inversion: mui ten phu thuoc chi tu ngoai vao
 *      trong (GhnCarrierAdapter -> ShippingCarrierPort), khong bao gio nguoc lai.
 *
 *   2. Chu ky ham chi dung kieu cua Domain (ShipmentRequest / ShippingQuote).
 *      Khong co HttpClient, khong co JSON, khong co token, khong co kieu du
 *      lieu nao cua SDK doi tac lot vao day.
 *
 *   3. Moi doi tac moi = MOT class moi implement interface nay. Use Case khong
 *      bi sua mot dong nao -> dung nguyen ly OCP ma de bai yeu cau.
 */
public interface ShippingCarrierPort {

    /** Nha van chuyen ma adapter nay dai dien. Factory dung de tra bang dang ky. */
    CarrierCode carrier();

    /**
     * Tinh phi cho mot kien hang.
     *
     * @throws CarrierUnavailableException khi khong goi duoc doi tac, hoac doi
     *         tac tu choi nhan don (qua tai trong, ngoai vung phuc vu, ...).
     */
    ShippingQuote calculateFee(ShipmentRequest request);
}
