package com.example.ordering.adapter;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;

/**
 * HA TANG HTTP + BANG DINH TUYEN REST.
 *
 * Trong Spring Boot, toan bo class nay duoc thay bang DispatcherServlet cong
 * cac annotation @GetMapping / @PostMapping. Viet tay de nhin ro co che:
 * URL + HTTP method -> mot phuong thuc cua Controller.
 *
 *   GET    /api/products
 *   GET    /api/products/{id}
 *   GET    /api/customers
 *   POST   /api/orders
 *   GET    /api/orders            (?status=NEW|PAID|CANCELLED)
 *   GET    /api/orders/{id}
 *   POST   /api/orders/{id}/pay
 *   POST   /api/orders/{id}/cancel
 */
public class RestServer {

    private final int port;
    private final ProductController productController;
    private final OrderController orderController;

    public RestServer(int port, ProductController productController, OrderController orderController) {
        this.port = port;
        this.productController = Objects.requireNonNull(productController);
        this.orderController = Objects.requireNonNull(orderController);
    }

    public void start() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        // this::handle duoc compiler goi thanh mot HttpHandler; JDK se goi nguoc lai
        // phuong thuc nay moi khi co request (inversion of control).
        server.createContext("/", this::handle);
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();

        System.out.println("REST API dang chay tai http://localhost:" + port);
        System.out.println("  GET    /api/products");
        System.out.println("  GET    /api/products/{id}");
        System.out.println("  GET    /api/customers");
        System.out.println("  POST   /api/orders");
        System.out.println("  GET    /api/orders?status=NEW|PAID|CANCELLED");
        System.out.println("  GET    /api/orders/{id}");
        System.out.println("  POST   /api/orders/{id}/pay");
        System.out.println("  POST   /api/orders/{id}/cancel");
        System.out.println("Nhan Ctrl+C de dung.");
    }

    private void handle(HttpExchange exchange) throws IOException {
        // Khoi catch phai nam BEN TRONG try-with-resources: try (exchange) dong
        // exchange truoc khi catch chay, nen de catch o ngoai thi response loi
        // se khong bao gio den duoc client.
        try (exchange) {
            try {
                send(exchange, dispatch(exchange));
            } catch (RuntimeException unexpected) {
                send(exchange, ApiResponse.error(500, "INTERNAL_ERROR", String.valueOf(unexpected)));
            }
        }
    }

    /** Tra URL ve dung phuong thuc Controller. */
    private ApiResponse dispatch(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        URI uri = exchange.getRequestURI();
        String path = normalize(uri.getPath());

        if (path.equals("/api/products")) {
            return method.equals("GET") ? productController.list() : ApiResponse.methodNotAllowed("GET");
        }
        if (path.startsWith("/api/products/")) {
            String id = path.substring("/api/products/".length());
            return method.equals("GET") ? productController.getOne(id) : ApiResponse.methodNotAllowed("GET");
        }
        if (path.equals("/api/customers")) {
            return method.equals("GET") ? productController.listCustomers() : ApiResponse.methodNotAllowed("GET");
        }
        if (path.equals("/api/orders")) {
            return switch (method) {
                case "GET" -> orderController.list(queryParam(uri.getRawQuery(), "status"));
                case "POST" -> orderController.create(readBody(exchange));
                default -> ApiResponse.methodNotAllowed("GET, POST");
            };
        }
        if (path.startsWith("/api/orders/")) {
            String rest = path.substring("/api/orders/".length());
            if (rest.endsWith("/pay")) {
                String id = rest.substring(0, rest.length() - "/pay".length());
                return method.equals("POST") ? orderController.pay(id) : ApiResponse.methodNotAllowed("POST");
            }
            if (rest.endsWith("/cancel")) {
                String id = rest.substring(0, rest.length() - "/cancel".length());
                return method.equals("POST") ? orderController.cancel(id) : ApiResponse.methodNotAllowed("POST");
            }
            return method.equals("GET") ? orderController.getOne(rest) : ApiResponse.methodNotAllowed("GET");
        }

        return ApiResponse.error(404, "NOT_FOUND", "Khong co endpoint " + method + " " + path);
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    /** Doc mot tham so tren query string, vi du ?status=PAID */
    private static String queryParam(String rawQuery, String name) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return null;
        }
        Map<String, String> params = new LinkedHashMap<>();
        for (String pair : rawQuery.split("&")) {
            int separator = pair.indexOf('=');
            if (separator > 0) {
                params.put(pair.substring(0, separator), pair.substring(separator + 1));
            }
        }
        return params.get(name);
    }

    private static String normalize(String path) {
        if (path.length() > 1 && path.endsWith("/")) {
            return path.substring(0, path.length() - 1);
        }
        return path;
    }

    private void send(HttpExchange exchange, ApiResponse response) throws IOException {
        byte[] payload = Json.write(response.body()).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        response.headers().forEach((name, value) -> exchange.getResponseHeaders().add(name, value));
        exchange.sendResponseHeaders(response.status(), payload.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(payload);
        }
    }
}
