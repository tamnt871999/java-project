package com.example.ordering.adapter;

import com.example.ordering.application.CustomerRepository;
import com.example.ordering.application.InMemoryCustomerRepository;
import com.example.ordering.application.InMemoryOrderRepository;
import com.example.ordering.application.InMemoryProductRepository;
import com.example.ordering.application.OrderRepository;
import com.example.ordering.application.OrderService;
import com.example.ordering.application.ProductRepository;
import com.example.ordering.application.ProductService;

import java.io.IOException;

/**
 * Diem khoi dong: khoi tao cac doi tuong theo tung tang roi noi lai voi nhau.
 *
 *   java com.example.ordering.adapter.Main        -> cong 8080
 *   java com.example.ordering.adapter.Main 9090   -> cong 9090
 *
 * Trong Spring Boot toan bo doan noi day duoc thay bang @Service /
 * @Repository / @RestController + tiem phu thuoc tu dong.
 */
public final class Main {

    private static final int DEFAULT_PORT = 8080;

    public static void main(String[] args) throws IOException {
        // Tang du lieu
        ProductRepository productRepository = new InMemoryProductRepository();
        CustomerRepository customerRepository = new InMemoryCustomerRepository();
        OrderRepository orderRepository = new InMemoryOrderRepository();

        // Tang nghiep vu
        ProductService productService = new ProductService(productRepository);
        OrderService orderService = new OrderService(orderRepository, productRepository, customerRepository);

        // Tang giao tiep (REST)
        ProductController productController = new ProductController(productService, customerRepository);
        OrderController orderController = new OrderController(orderService);

        int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        new RestServer(port, productController, orderController).start();
    }

    private Main() {
    }
}
