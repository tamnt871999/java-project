package com.example.logistics.bootstrap;

import com.example.logistics.core.factory.CarrierRoutingPolicy;
import com.example.logistics.core.factory.ShippingCarrierFactory;
import com.example.logistics.core.port.in.CalculateShippingFeePort;
import com.example.logistics.core.port.out.ShippingCarrierPort;
import com.example.logistics.core.usecase.CalculateShippingFeeUseCase;
import com.example.logistics.infrastructure.carrier.ghn.GhnCarrierAdapter;
import com.example.logistics.infrastructure.carrier.ghtk.GhtkCarrierAdapter;
import com.example.logistics.infrastructure.cli.ShippingFeeCliAdapter;

import java.util.List;

/**
 * COMPOSITION ROOT - noi duy nhat trong he thong duoc phep biet ca hai the gioi.
 *
 * Trach nhiem DUY NHAT cua file nay la LAP RAP: dung adapter, nap luat, tiem
 * phu thuoc, roi trao quyen dieu khien. Khong mot dong nghiep vu, khong mot
 * dong trinh bay nao o day - phan in an da nam o ShippingFeeCliAdapter.
 *
 * Cung vi vay day la file duy nhat duoc phep goi "new GhnCarrierAdapter(...)".
 * Doi lai, moi file trong package core deu sach bong khoi ten doi tac va ten
 * cong nghe. Trong Spring Boot, doan duoi duoc thay bang @Component + @Bean +
 * tiem tu dong, nhung y tuong khong doi: viec lap rap nam o ria, khong nam
 * trong loi.
 *
 * Cach chay:
 *   java com.example.logistics.bootstrap.Main                 -> chay bo demo
 *   java com.example.logistics.bootstrap.Main "Ha Noi" 1200   -> bao gia 1 don
 */
public final class Main {

    public static void main(String[] args) {
        // --- 1. Dung cac DRIVEN adapter (tang Infrastructure) ------------------
        // Token va URL la cau hinh ha tang, thuc te doc tu bien moi truong.
        ShippingCarrierPort ghn = new GhnCarrierAdapter("GHN-TOKEN-DEMO");
        ShippingCarrierPort ghtk = new GhtkCarrierAdapter("https://services.giaohangtietkiem.vn");

        // --- 2. Nap luat dinh tuyen va chon chien luoc chon hang (tang Core) ---
        CarrierRoutingPolicy routingPolicy = CarrierRoutingPolicy.defaultPolicy();
        ShippingCarrierFactory carrierFactory =
                new ShippingCarrierFactory(routingPolicy, List.of(ghn, ghtk));

        // --- 3. Tiem chien luoc vao Use Case qua constructor -------------------
        CalculateShippingFeePort calculateShippingFee =
                new CalculateShippingFeeUseCase(carrierFactory);

        // --- 4. Cam DRIVING adapter vao inbound port va trao quyen dieu khien --
        new ShippingFeeCliAdapter(calculateShippingFee, routingPolicy,
                carrierFactory.registeredCarriers()).run(args);
    }

    private Main() {
    }
}
