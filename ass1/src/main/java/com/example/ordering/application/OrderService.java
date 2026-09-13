package com.example.ordering.application;

import com.example.ordering.domain.Customer;
import com.example.ordering.domain.Money;
import com.example.ordering.domain.Order;
import com.example.ordering.domain.OrderLine;
import com.example.ordering.domain.OrderStatus;
import com.example.ordering.domain.Product;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * SERVICE - noi chua TOAN BO nghiep vu dat hang.
 *
 * Trong MVC, day la tang ma Controller goi vao. Controller khong duoc phep
 * chua mot dong quy tac kinh doanh nao; no chi nhan input, goi Service, roi
 * chon View de hien thi.
 *
 * Service phu thuoc TRUC TIEP vao Repository va Model - khong co port, khong
 * co dependency inversion. Do la dac trung cua kien truc phan tang truyen thong.
 */
public class OrderService {

    /** Giam 10% khi tam tinh dat nguong nay. */
    private static final Money DISCOUNT_THRESHOLD = Money.of("500.00");
    private static final int DISCOUNT_PERCENT = 10;
    /** Mien phi ship khi tam tinh dat nguong nay. */
    private static final Money FREE_SHIPPING_THRESHOLD = Money.of("100.00");
    private static final Money SHIPPING_FEE = Money.of("9.99");
    /** Cong thanh toan tu choi don qua lon. */
    private static final Money PAYMENT_LIMIT = Money.of("1000.00");

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;

    public OrderService(OrderRepository orderRepository,
                        ProductRepository productRepository,
                        CustomerRepository customerRepository) {
        this.orderRepository = Objects.requireNonNull(orderRepository);
        this.productRepository = Objects.requireNonNull(productRepository);
        this.customerRepository = Objects.requireNonNull(customerRepository);
    }

    /**
     * Dat hang: kiem tra - dung don - tinh gia - tru kho - luu.
     */
    public Order placeOrder(PlaceOrderRequest request) {
        // 1. Kiem tra du lieu dau vao
        if (request.getCustomerId() == null || request.getCustomerId().isBlank()) {
            throw ServiceException.invalid("Vui long chon khach hang");
        }
        if (request.getShippingAddress() == null || request.getShippingAddress().isBlank()) {
            throw ServiceException.invalid("Vui long nhap dia chi giao hang");
        }
        if (request.getItems().isEmpty()) {
            throw ServiceException.invalid("Vui long chon it nhat mot san pham");
        }

        // 2. Kiem tra khach hang
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> ServiceException.notFound(
                        "Khong tim thay khach hang: " + request.getCustomerId()));
        if (!customer.isActive()) {
            throw ServiceException.invalid("Khach hang dang bi khoa: " + customer.getName());
        }

        // 3. Kiem tra ton kho TRUOC khi tru bat ky mon nao (tranh tru nua chung)
        for (Map.Entry<String, Integer> item : request.getItems().entrySet()) {
            Product product = findProduct(item.getKey());
            if (!product.hasEnoughStock(item.getValue())) {
                // Nem SO LIEU tho, khong nem chuoi da dinh dang san.
                throw ServiceException.outOfStock(
                        product.getId(), item.getValue(), product.getStock());
            }
        }

        // 4. Dung don hang. Gia LUON lay tu database, khong tin gia client gui len.
        Order order = new Order(orderRepository.nextId(), customer.getId(), customer.getName(),
                request.getShippingAddress().trim(), LocalDateTime.now());
        for (Map.Entry<String, Integer> item : request.getItems().entrySet()) {
            Product product = findProduct(item.getKey());
            order.addLine(new OrderLine(product.getId(), product.getName(),
                    product.getPrice(), item.getValue()));
        }

        // 5. Ap dung chinh sach gia
        calculatePricing(order);

        // 6. Tru kho va luu
        for (Map.Entry<String, Integer> item : request.getItems().entrySet()) {
            Product product = findProduct(item.getKey());
            product.decreaseStock(item.getValue());
            productRepository.save(product);
        }
        orderRepository.save(order);
        return order;
    }

    /** Thanh toan don hang. */
    public Order pay(String orderId) {
        Order order = findById(orderId);
        if (order.getTotal().isAtLeast(PAYMENT_LIMIT)) {
            throw ServiceException.paymentDeclined(
                    order.getTotal().amount(), PAYMENT_LIMIT.amount());
        }
        order.markPaid();
        orderRepository.save(order);
        return order;
    }

    /** Huy don va tra hang ve kho. */
    public Order cancel(String orderId, String reason) {
        Order order = findById(orderId);
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw ServiceException.invalid("Don hang nay da bi huy truoc do");
        }
        for (OrderLine line : order.getLines()) {
            productRepository.findById(line.getProductId()).ifPresent(product -> {
                product.increaseStock(line.getQuantity());
                productRepository.save(product);
            });
        }
        order.cancel(reason);
        orderRepository.save(order);
        return order;
    }

    public Order findById(String orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> ServiceException.notFound("Khong tim thay don hang: " + orderId));
    }

    public List<Order> findAll() {
        return orderRepository.findAll();
    }

    /** Loc don theo trang thai - phuc vu GET /api/orders?status=PAID */
    public List<Order> findByStatus(OrderStatus status) {
        Objects.requireNonNull(status, "status must not be null");
        return orderRepository.findAll().stream()
                .filter(order -> order.getStatus() == status)
                .toList();
    }

    /**
     * CHINH SACH GIA - dat o Service chu khong o Model, vi no thay doi theo
     * chien dich kinh doanh chu khong phai thuoc tinh cua don hang.
     */
    private void calculatePricing(Order order) {
        Money subtotal = order.getSubtotal();
        Money discount = subtotal.isAtLeast(DISCOUNT_THRESHOLD)
                ? subtotal.percent(DISCOUNT_PERCENT)
                : Money.ZERO;
        Money shippingFee = subtotal.isAtLeast(FREE_SHIPPING_THRESHOLD)
                ? Money.ZERO
                : SHIPPING_FEE;
        order.applyPricing(discount, shippingFee);
    }

    private Product findProduct(String productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> ServiceException.notFound("Khong tim thay san pham: " + productId));
    }
}
