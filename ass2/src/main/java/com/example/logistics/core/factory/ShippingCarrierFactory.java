package com.example.logistics.core.factory;

import com.example.logistics.core.domain.CarrierCode;
import com.example.logistics.core.domain.City;
import com.example.logistics.core.domain.DomainException;
import com.example.logistics.core.domain.ShipmentRequest;
import com.example.logistics.core.port.out.CarrierUnavailableException;
import com.example.logistics.core.port.out.ShippingCarrierPort;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * FACTORY + STRATEGY - yeu cau d.
 *
 * Nhiem vu: nhin vao thanh pho nhan hang, tra ve DUNG mot ShippingCarrierPort
 * de Use Case goi, ma KHONG he biet ben duoi la GHN SDK hay GHTK REST.
 *
 * CACH GIU CHO LOI KHONG BI RO RI CHI TIET HA TANG:
 *
 *   - Factory nam o Core nhung khong he goi "new GhnCarrierAdapter()". Neu no
 *     goi new thi package core se phai import package infrastructure, va toan
 *     bo kien truc Hexagonal sup do ngay dong import do.
 *
 *   - Thay vao do cac adapter da dung san duoc TIEM VAO qua constructor tu
 *     Composition Root (lop Main). Factory chi giu mot bang tra:
 *         CarrierCode -> ShippingCarrierPort
 *     Ca hai kieu nay deu thuoc ve Core, nen Core chi phu thuoc vao chinh no.
 *
 *   - Viec "thanh pho nao thi doi tac nao" duoc tach han sang CarrierRoutingPolicy,
 *     vi do la luat kinh doanh de thay doi nhat.
 *
 * Ket qua: them ViettelPost = viet them 1 adapter + them 1 dong dang ky o Main.
 * Khong sua Factory, khong sua Use Case, khong sua Port. Do la OCP.
 *
 * Day chi la MOT chien luoc chon hang (tra bang theo tinh / thanh). Muon doi
 * sang chien luoc khac thi viet ban hien thuc khac cua ShippingCarrierProvider,
 * Use Case van giu nguyen.
 */
public final class ShippingCarrierFactory implements ShippingCarrierProvider {

    private final CarrierRoutingPolicy routingPolicy;
    private final Map<CarrierCode, ShippingCarrierPort> registry;

    public ShippingCarrierFactory(CarrierRoutingPolicy routingPolicy,
                                  Collection<ShippingCarrierPort> carriers) {
        this.routingPolicy = Objects.requireNonNull(routingPolicy, "routingPolicy must not be null");
        Objects.requireNonNull(carriers, "carriers must not be null");
        if (carriers.isEmpty()) {
            throw new DomainException("Phai dang ky it nhat mot nha van chuyen");
        }
        Map<CarrierCode, ShippingCarrierPort> map = new LinkedHashMap<>();
        for (ShippingCarrierPort carrier : carriers) {
            Objects.requireNonNull(carrier, "carrier adapter must not be null");
            ShippingCarrierPort previous = map.put(carrier.carrier(), carrier);
            if (previous != null) {
                throw new DomainException("Dang ky trung nha van chuyen: " + carrier.carrier());
            }
        }
        this.registry = Collections.unmodifiableMap(map);
    }

    /** Loi tat: dung chinh sach dinh tuyen mac dinh cua de bai. */
    public static ShippingCarrierFactory withDefaultPolicy(ShippingCarrierPort... carriers) {
        return new ShippingCarrierFactory(CarrierRoutingPolicy.defaultPolicy(), Set.of(carriers));
    }

    /**
     * Chon chien luoc van chuyen cho thanh pho nay.
     *
     * Toan bo doan if-else ma de bai canh bao ("neu thanh pho la X thi goi GHN")
     * bi don het vao day duoi dang bang tra, va nam ngoai Use Case.
     */
    @Override
    public ShippingCarrierPort carrierFor(ShipmentRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        City destination = request.getDestination();
        CarrierCode code = routingPolicy.carrierFor(destination);
        ShippingCarrierPort carrier = registry.get(code);
        if (carrier == null) {
            // Luat dinh tuyen tro toi mot doi tac chua duoc dang ky adapter -
            // loi cau hinh o Composition Root chu khong phai loi nguoi dung.
            throw new CarrierUnavailableException(code,
                    "Chua dang ky adapter cho nha van chuyen " + code
                            + " (thanh pho: " + destination.displayName() + ")");
        }
        return carrier;
    }

    public Set<CarrierCode> registeredCarriers() {
        return registry.keySet();
    }

    public CarrierRoutingPolicy routingPolicy() {
        return routingPolicy;
    }
}
