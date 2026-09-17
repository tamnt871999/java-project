package com.example.ordering.infrastructure.web;

import com.example.ordering.adapter.in.web.ApiResponse;
import com.example.ordering.adapter.in.web.JsonSerializer;
import com.example.ordering.adapter.in.web.OrderController;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.Executors;

/**
 * FRAMEWORKS AND DRIVERS - ha tang HTTP va bang dinh tuyen.
 *
 * Trong Spring Boot, toan bo class nay duoc thay bang DispatcherServlet cong
 * cac annotation @PostMapping / @GetMapping. Viet tay de nhin ro co che ben
 * duoi: URL cong HTTP method tro toi mot phuong thuc cua Controller.
 *
ROUTES:
 *   POST /orders       -> OrderController.placeOrder(body)
 *   GET  /orders/{id}  -> OrderController.getOrder(id)
 *
 * Class nay khong biet gi ve nghiep vu. No chi doc byte, goi Controller, roi
 * ghi byte tra ve.
 */
public class HttpServerRunner {

    private final int port;
    private final OrderController orderController;
    private HttpServer server;

    public HttpServerRunner(int port, OrderController orderController) {
        this.port = port;
        this.orderController = Objects.requireNonNull(orderController, "orderController must not be null");
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        // this::handle duoc compiler goi thanh mot HttpHandler. Khong dong code
        // nao cua ta goi handle() ca - JDK goi nguoc lai (inversion of control).
        server.createContext("/", this::handle);
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();

        System.out.println("REST API dang chay tai http://localhost:" + port);
        System.out.println("  POST /orders        dat hang");
        System.out.println("  GET  /orders/{id}   xem chi tiet don");
        System.out.println("Nhan Ctrl+C de dung.");
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    private void handle(HttpExchange exchange) throws IOException {
        // Khoi catch phai nam BEN TRONG try-with-resources: try (exchange) dong
        // exchange truoc khi catch chay, nen de catch o ngoai thi response loi
        // khong bao gio den duoc client.
        try (exchange) {
            try {
                send(exchange, route(exchange));
            } catch (RuntimeException unexpected) {
                send(exchange, ApiResponse.error(500, "INTERNAL_ERROR", String.valueOf(unexpected)));
            }
        }
    }

    private ApiResponse route(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = normalize(exchange.getRequestURI().getPath());

        if (path.equals("/orders")) {
            if (method.equals("POST")) {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                return orderController.placeOrder(body);
            }
            return ApiResponse.methodNotAllowed("POST");
        }

        // GET /orders/{id} - boc bien duong dan ra khoi URL.
        // Spring lam viec nay bang @GetMapping("/orders/{id}") cong
        // @PathVariable; o day ta cat chuoi bang tay cho thay ro co che.
        if (path.startsWith(ORDERS_PREFIX)) {
            String orderId = path.substring(ORDERS_PREFIX.length());
            if (!orderId.isBlank() && !orderId.contains("/")) {
                if (method.equals("GET")) {
                    return orderController.getOrder(decode(orderId));
                }
                return ApiResponse.methodNotAllowed("GET");
            }
        }
        return ApiResponse.error(404, "NOT_FOUND", "Khong co endpoint " + method + " " + path);
    }

    private static final String ORDERS_PREFIX = "/orders/";

    /** Ma don co the chua ky tu da duoc percent-encode tren URL. */
    private static String decode(String segment) {
        return URLDecoder.decode(segment, StandardCharsets.UTF_8);
    }

    private static String normalize(String path) {
        if (path.length() > 1 && path.endsWith("/")) {
            return path.substring(0, path.length() - 1);
        }
        return path;
    }

    private void send(HttpExchange exchange, ApiResponse response) throws IOException {
        byte[] payload = JsonSerializer.toJson(response.body()).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        response.headers().forEach((name, value) -> exchange.getResponseHeaders().add(name, value));
        exchange.sendResponseHeaders(response.status(), payload.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(payload);
        }
    }
}
