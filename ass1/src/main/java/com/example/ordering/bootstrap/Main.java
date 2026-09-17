package com.example.ordering.bootstrap;

import com.example.ordering.adapter.in.web.OrderController;
import com.example.ordering.adapter.out.persistence.JpaOrderRepositoryAdapter;
import com.example.ordering.adapter.out.persistence.GeneratedOrderJpaRepository;
import com.example.ordering.adapter.out.persistence.OrderJpaRepository;
import com.example.ordering.application.port.in.GetOrderUseCase;
import com.example.ordering.application.port.in.PlaceOrderUseCase;
import com.example.ordering.application.port.out.OrderRepository;
import com.example.ordering.application.usecase.GetOrderService;
import com.example.ordering.application.usecase.PlaceOrderService;
import com.example.ordering.domain.OrderPricingService;
import com.example.ordering.infrastructure.db.Database;
import com.example.ordering.infrastructure.web.HttpServerRunner;

import java.io.IOException;
import java.time.Clock;

/**
 * COMPOSITION ROOT - noi DUY NHAT trong he thong duoc phep biet ca hai the gioi:
 * cac use case truu tuong va cac ban hien thuc cu the.
 *
 * Doc tu duoi len se thay dung thu tu cac vong cua Clean Architecture:
 *   Frameworks and Drivers -> Interface Adapters -> Use Cases -> Entities
 *
 * Muon doi H2 sang Postgres that? Doi dong tao Database. Muon doi REST sang
 * gRPC? Doi dong tao HttpServerRunner. Khong mot file nao trong domain hay
 * application phai sua - do la loi hua cua Dependency Rule.
 *
 * Trong Spring Boot, ca ham nay duoc thay bang @Component / @Service /
 * @RestController cong tiem phu thuoc tu dong, nhung y tuong khong doi: viec
 * LAP RAP nam o ria, khong nam trong loi.
 *
 * Cach chay:
 *   java com.example.ordering.bootstrap.Main        -> cong 8080
 *   java com.example.ordering.bootstrap.Main 9090   -> cong 9090
 */
public final class Main {

    private static final int DEFAULT_PORT = 8080;

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;

        // --- Vong 4: FRAMEWORKS AND DRIVERS -----------------------------------
        // Database dong vai H2/Postgres; SimpleJpaRepository dong vai Spring Data JPA.
        Database database = new Database("H2", true);
        OrderJpaRepository jpaRepository = new GeneratedOrderJpaRepository(database);

        // --- Vong 3: INTERFACE ADAPTERS ---------------------------------------
        OrderRepository orderRepository = new JpaOrderRepositoryAdapter(jpaRepository);

        // --- Vong 2 va 1: USE CASES va ENTITIES -------------------------------
        OrderPricingService pricingService = new OrderPricingService();
        PlaceOrderUseCase placeOrderUseCase =
                new PlaceOrderService(pricingService, orderRepository, Clock.systemUTC());
        // Hai use case dung CHUNG mot orderRepository: cung mot cong ra, hai
        // huong su dung khac nhau. Ghi di qua domain service va aggregate;
        // doc thi di thang, vi khong co quy tac nghiep vu nao de ap dung.
        GetOrderUseCase getOrderUseCase = new GetOrderService(orderRepository);

        // --- Quay lai vong 3 roi vong 4: cam adapter vao ha tang --------------
        OrderController orderController = new OrderController(placeOrderUseCase, getOrderUseCase);
        new HttpServerRunner(port, orderController).start();
    }


    private Main() {
    }
}
