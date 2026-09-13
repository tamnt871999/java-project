package com.example.logistics.core.factory;

import com.example.logistics.core.domain.ShipmentRequest;
import com.example.logistics.core.port.out.ShippingCarrierPort;

/**
 * Truu tuong hoa viec CHON nha van chuyen.
 *
 * Vi sao Use Case phai phu thuoc vao interface nay chu khong phai vao thang
 * class ShippingCarrierFactory?
 *
 * Ca hai deu nam trong Core nen ve luat phu thuoc thi khong sai. Nhung neu
 * Use Case om class cu the, chien luoc chon hang bi dong cung o mot cach duy
 * nhat: tra bang theo tinh / thanh. Den luc kinh doanh yeu cau khac di, ta lai
 * phai mo file dang chay on dinh ra sua - dung vao cho ma OCP cam sua.
 *
 * Co interface nay thi moi chien luoc chon hang la mot ban hien thuc rieng:
 *
 *   ShippingCarrierFactory      - tra bang theo tinh / thanh (mac dinh)
 *   HeavyParcelCarrierProvider  - kien qua nang thi tranh hang khong nhan
 *   PeakSeasonCarrierProvider   - mua cao diem thi don sang hang con tai
 *
 * Use Case khong doi mot dong nao trong ca ba truong hop.
 *
 * THAM SO LA CA YEU CAU GIAO HANG, khong phai moi moi City: chien luoc chon
 * hang thuong can biet ca trong luong (GHTK tu choi kien tren 20kg) chu khong
 * chi diem den. Truyen ca ShipmentRequest de khong khoa cung kha nang mo rong.
 */
public interface ShippingCarrierProvider {

    /**
     * Chon adapter phu trach yeu cau giao hang nay.
     *
     * @throws com.example.logistics.core.port.out.CarrierUnavailableException
     *         khi khong co nha van chuyen nao phuc vu duoc yeu cau.
     */
    ShippingCarrierPort carrierFor(ShipmentRequest request);
}
