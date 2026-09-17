package com.example.ordering;

import com.example.ordering.adapter.in.web.ApiResponse;
import com.example.ordering.adapter.in.web.OrderController;
import com.example.ordering.adapter.out.persistence.GeneratedOrderJpaRepository;
import com.example.ordering.adapter.out.persistence.JpaOrderRepositoryAdapter;
import com.example.ordering.application.port.in.GetOrderUseCase;
import com.example.ordering.application.port.in.OrderNotFoundException;
import com.example.ordering.application.port.in.OrderView;
import com.example.ordering.application.port.in.PlaceOrderCommand;
import com.example.ordering.application.port.in.PlaceOrderResult;
import com.example.ordering.application.port.in.PlaceOrderUseCase;
import com.example.ordering.application.port.out.OrderRepository;
import com.example.ordering.application.usecase.GetOrderService;
import com.example.ordering.application.usecase.PlaceOrderService;
import com.example.ordering.domain.DomainException;
import com.example.ordering.domain.Money;
import com.example.ordering.domain.OrderItem;
import com.example.ordering.domain.OrderPricingService;
import com.example.ordering.domain.PriceBreakdown;
import com.example.ordering.infrastructure.db.Database;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Bo kiem thu tu viet - khong dung JUnit de bai tap chay duoc chi voi JDK 21.
 *
 * Diem dang chu y: KHONG mot bai test nao duoi day can mo cong HTTP hay dung
 * database that. Do la phan thuong truc tiep cua Clean Architecture - use case
 * chi phu thuoc interface nen thay gi cung duoc.
 */
public final class SelfCheck {

    private static final Instant NOW = Instant.parse("2026-01-15T10:00:00Z");
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        tinhTienCoBan();
        mienPhiShipTuNguong100();
        giamGia10PhanTramTuNguong500();
        donHangRongBiTuChoi();
        trungSanPhamBiTuChoi();
        soLuongKhongHopLeBiTuChoi();
        useCaseTraVeDtoChuKhongPhaiAggregate();
        controllerDichLoiThanhMaHttp();
        luongChayDungThuTuSequenceDiagram();
        docLaiDonVuaTao();
        docDonKhongTonTaiNemOrderNotFound();
        controllerDichOrderNotFoundThanh404();
        dtoDocTachRoiDtoGhi();
        luongDocChiGoiDungMotMessage();
        ArchitectureFitness.run();

        System.out.println();
        System.out.println("Ket qua: " + passed + " dat / " + failed + " loi");
        if (failed > 0) {
            System.exit(1);
        }
    }

    // --- ENTITIES ----------------------------------------------------------

    private static void tinhTienCoBan() {
        PriceBreakdown price = new OrderPricingService().calculateTotal(List.of(
                OrderItem.of("SKU-A", 3, "129.00"),
                OrderItem.of("SKU-B", 2, "12.00")));
        check("tinh tam tinh = 3x129 + 2x12", Money.of("411.00"), price.subtotal());
        check("don tren 100 duoc mien phi ship", Money.ZERO, price.shippingFee());
        check("tong cong bang tam tinh", Money.of("411.00"), price.total());
    }

    private static void mienPhiShipTuNguong100() {
        PriceBreakdown duoiNguong = new OrderPricingService()
                .calculateTotal(List.of(OrderItem.of("SKU-A", 1, "99.99")));
        check("duoi 100 thi thu phi ship 9.99", Money.of("9.99"), duoiNguong.shippingFee());

        PriceBreakdown dungNguong = new OrderPricingService()
                .calculateTotal(List.of(OrderItem.of("SKU-A", 1, "100.00")));
        check("dung 100 la duoc mien phi ship", Money.ZERO, dungNguong.shippingFee());
    }

    private static void giamGia10PhanTramTuNguong500() {
        PriceBreakdown duoiNguong = new OrderPricingService()
                .calculateTotal(List.of(OrderItem.of("SKU-A", 1, "499.99")));
        check("499.99 chua duoc giam gia", Money.ZERO, duoiNguong.discount());

        PriceBreakdown dungNguong = new OrderPricingService()
                .calculateTotal(List.of(OrderItem.of("SKU-A", 2, "250.00")));
        check("dung 500 duoc giam 10 phan tram", Money.of("50.00"), dungNguong.discount());
        check("tong sau giam gia", Money.of("450.00"), dungNguong.total());
    }

    private static void donHangRongBiTuChoi() {
        expectDomainException("don hang rong bi tu choi",
                () -> new OrderPricingService().calculateTotal(List.of()));
    }

    private static void trungSanPhamBiTuChoi() {
        Fixture fixture = new Fixture();
        expectDomainException("trung san pham bi aggregate chan", () ->
                fixture.placeOrderUseCase.placeOrder(new PlaceOrderCommand("CUS-1", List.of(
                        new PlaceOrderCommand.Item("SKU-A", 1, "10.00"),
                        new PlaceOrderCommand.Item("SKU-A", 2, "10.00")))));
    }

    private static void soLuongKhongHopLeBiTuChoi() {
        Fixture fixture = new Fixture();
        expectDomainException("so luong 0 bi Value Object chan", () ->
                fixture.placeOrderUseCase.placeOrder(new PlaceOrderCommand("CUS-1", List.of(
                        new PlaceOrderCommand.Item("SKU-A", 0, "10.00")))));
    }

    // --- USE CASES ---------------------------------------------------------

    private static void useCaseTraVeDtoChuKhongPhaiAggregate() {
        Fixture fixture = new Fixture();
        PlaceOrderResult result = fixture.placeOrder("CUS-1", "SKU-A", 3, "129.00");

        check("result mang ma don", "ORD-1001", result.orderId());
        check("result mang tong tien", new BigDecimal("387.00"), result.total());
        check("kieu tra ve la DTO bien chu khong phai aggregate",
                PlaceOrderResult.class, result.getClass());
        // Sequence diagram ghi Result(orderId, total) - dung hai truong, khong hon.
        check("DTO tra ve dung hai truong nhu trong hinh",
                2, PlaceOrderResult.class.getRecordComponents().length);
    }

    // --- INTERFACE ADAPTERS ------------------------------------------------

    private static void controllerDichLoiThanhMaHttp() {
        Fixture fixture = new Fixture();
        OrderController controller =
                new OrderController(fixture.placeOrderUseCase, fixture.getOrderUseCase);

        ApiResponse created = controller.placeOrder(json("CUS-1", "SKU-A", 2));
        check("dat hang thanh cong tra 201", 201, created.status());

        check("JSON hong tra 400", 400, controller.placeOrder("{khong phai json").status());
        check("thieu truong bat buoc tra 400", 400, controller.placeOrder("{\"items\":[]}").status());
        check("vi pham quy tac nghiep vu tra 422", 422,
                controller.placeOrder(json("CUS-1", "SKU-A", 0)).status());
    }

    // --- SEQUENCE DIAGRAM --------------------------------------------------

    /**
     * Kiem chung luong chay dien ra DUNG TRINH TU ma sequence diagram mo ta.
     *
     * Cac lifeline duoc boc bang lop vo ghi lai ten loi goi; sau do ta so sanh
     * danh sach thu duoc voi thu tu mong doi. Diagram ve tren giay khong ai bat
     * buoc phai dung, con bai test nay thi co.
     */
    private static void luongChayDungThuTuSequenceDiagram() {
        Database database = new Database("H2", false);
        OrderRepository realRepository =
                new JpaOrderRepositoryAdapter(new GeneratedOrderJpaRepository(database));

        SequenceRecorder recorder = new SequenceRecorder();
        PlaceOrderUseCase useCase = new PlaceOrderService(
                recorder.pricingService(),
                recorder.orderRepository(realRepository),
                Clock.fixed(NOW, ZoneOffset.UTC));

        new OrderController(useCase, new GetOrderService(realRepository))
                .placeOrder(json("CUS-1", "SKU-A", 1));

        // Dung hai message ma sequence diagram ve cho PlaceOrderService, dung
        // thu tu do. Them bat ky loi goi nao khac la bai test nay do.
        List<String> mongDoi = List.of(
                "OrderPricingService.calculateTotal",
                "OrderRepository.save");
        check("luong chay khop sequence diagram", mongDoi, recorder.calls());
    }

    // --- LUONG DOC: GET /orders/{id} ---------------------------------------

    /**
     * Ghi roi doc lai: du lieu phai di tron mot vong xuong bang roi quay len
     * nguyen ven, qua day du cac phep dich domain -> entity -> row -> entity
     * -> domain -> DTO.
     */
    private static void docLaiDonVuaTao() {
        Fixture fixture = new Fixture();
        PlaceOrderResult created = fixture.placeOrder("CUS-7", "SKU-A", 3, "129.00");

        OrderView view = fixture.getOrderUseCase.getOrder(created.orderId());

        check("doc lai dung ma don", created.orderId(), view.orderId());
        check("doc lai dung khach hang", "CUS-7", view.customerId());
        check("doc lai dung trang thai", "PLACED", view.status());
        check("doc lai dung tong tien", created.total(), view.total());
        check("doc lai du so dong hang", 1, view.items().size());
        check("doc lai dung thanh tien tung dong",
                new BigDecimal("387.00"), view.items().get(0).lineTotal());
    }

    /** Khong tim thay la loi cua APPLICATION, khong phai cua domain. */
    private static void docDonKhongTonTaiNemOrderNotFound() {
        Fixture fixture = new Fixture();
        try {
            fixture.getOrderUseCase.getOrder("ORD-KHONG-CO");
            check("doc don khong ton tai phai nem OrderNotFoundException", "nem", "khong nem");
        } catch (OrderNotFoundException expected) {
            check("doc don khong ton tai nem OrderNotFoundException",
                    "ORD-KHONG-CO", expected.orderId());
        }
    }

    /**
     * Use case nem exception nghiep vu; CHI Controller biet no thanh so 404.
     * Grep chu "404" trong ca application lan domain se khong ra ket qua nao.
     */
    private static void controllerDichOrderNotFoundThanh404() {
        Fixture fixture = new Fixture();
        OrderController controller =
                new OrderController(fixture.placeOrderUseCase, fixture.getOrderUseCase);

        ApiResponse created = controller.placeOrder(json("CUS-1", "SKU-A", 2));
        check("dat hang truoc khi doc tra 201", 201, created.status());

        check("doc don co that tra 200", 200, controller.getOrder("ORD-1001").status());
        check("doc don khong co tra 404", 404, controller.getOrder("ORD-9999").status());
    }

    /**
     * DTO doc va DTO ghi la HAI hop dong khac nhau, co y khong dung chung.
     * Neu gop lam mot thi them truong cho man hinh chi tiet se lam phinh
     * response cua API tao don.
     */
    private static void dtoDocTachRoiDtoGhi() {
        check("DTO ghi van dung hai truong nhu sequence diagram",
                2, PlaceOrderResult.class.getRecordComponents().length);
        check("DTO doc chi tiet hon DTO ghi", true,
                OrderView.class.getRecordComponents().length
                        > PlaceOrderResult.class.getRecordComponents().length);
    }

    /**
     * Luong doc goi DUNG MOT message ra ngoai: findById. Khong dung toi domain
     * service, vi doc lai don khong phai la luc tinh lai gia.
     */
    private static void luongDocChiGoiDungMotMessage() {
        Database database = new Database("H2", false);
        OrderRepository realRepository =
                new JpaOrderRepositoryAdapter(new GeneratedOrderJpaRepository(database));
        new PlaceOrderService(new OrderPricingService(), realRepository,
                Clock.fixed(NOW, ZoneOffset.UTC))
                .placeOrder(new PlaceOrderCommand("CUS-1",
                        List.of(new PlaceOrderCommand.Item("SKU-A", 1, "50.00"))));

        SequenceRecorder recorder = new SequenceRecorder();
        new GetOrderService(recorder.orderRepository(realRepository)).getOrder("ORD-1001");

        check("luong doc chi goi findById", List.of("OrderRepository.findById"), recorder.calls());
    }

    // --- ha tang cho bo test -----------------------------------------------

    /**
     * Rap san mot he thong day du nhung chay hoan toan trong bo nho.
     *
     * Day la bang chung cua Dependency Rule: doi ca tang luu tru ma khong mot
     * dong nao cua use case hay domain phai sua.
     */
    private static final class Fixture {
        private final PlaceOrderUseCase placeOrderUseCase;
        private final GetOrderUseCase getOrderUseCase;

        private Fixture() {
            Database database = new Database("H2", false);
            OrderRepository repository =
                    new JpaOrderRepositoryAdapter(new GeneratedOrderJpaRepository(database));
            this.placeOrderUseCase = new PlaceOrderService(
                    new OrderPricingService(), repository, Clock.fixed(NOW, ZoneOffset.UTC));
            // Hai use case, CHUNG mot repository - dung nhu trong Main.
            this.getOrderUseCase = new GetOrderService(repository);
        }

        private PlaceOrderResult placeOrder(String customerId, String productId,
                                            int quantity, String unitPrice) {
            return placeOrderUseCase.placeOrder(new PlaceOrderCommand(customerId,
                    List.of(new PlaceOrderCommand.Item(productId, quantity, unitPrice))));
        }
    }

    private static String json(String customerId, String productId, int quantity) {
        return ("{'customerId':'%s','items':[{'productId':'%s','quantity':%d,'unitPrice':50.00}]}")
                .formatted(customerId, productId, quantity).replace('\'', '"');
    }

    private static void expectDomainException(String name, Runnable action) {
        try {
            action.run();
            fail(name + " - le ra phai nem DomainException");
        } catch (DomainException e) {
            pass(name);
        }
    }

    private static void check(String name, Object expected, Object actual) {
        if (expected.equals(actual)) {
            pass(name);
        } else {
            fail(name + " - mong doi <" + expected + "> nhung nhan <" + actual + ">");
        }
    }

    static void pass(String name) {
        passed++;
        System.out.println("  [OK]   " + name);
    }

    static void fail(String message) {
        failed++;
        System.out.println("  [LOI]  " + message);
    }

    private SelfCheck() {
    }
}
