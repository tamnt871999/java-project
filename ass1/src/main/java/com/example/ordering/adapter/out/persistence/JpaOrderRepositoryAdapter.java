package com.example.ordering.adapter.out.persistence;

import com.example.ordering.application.port.out.OrderRepository;
import com.example.ordering.domain.Order;
import com.example.ordering.domain.OrderId;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * OUTBOUND ADAPTER - lifeline "JpaOrderRepositoryAdapter (Outbound Adapter /
 * Interface Adapters)" trong sequence diagram.
 *
 * Vai tro: ban le giua hai the gioi.
 *   - Ngoanh vao trong: hien thuc OrderRepository, chi noi bang ngon ngu domain.
 *   - Ngoanh ra ngoai : goi Spring Data JPA, noi bang ngon ngu OrderEntity.
 *
 * Nho co lop nay ma use case khong bao gio nhin thay chu "Jpa" hay "Entity".
 * Doi sang MongoDB chi la viet mot adapter khac roi doi mot dong trong Main.
 */
public class JpaOrderRepositoryAdapter implements OrderRepository {

    private final OrderJpaRepository jpaRepository;
    private final AtomicInteger sequence = new AtomicInteger(1000);

    public JpaOrderRepositoryAdapter(OrderJpaRepository jpaRepository) {
        this.jpaRepository = Objects.requireNonNull(jpaRepository, "jpaRepository must not be null");
    }

    @Override
    public Order save(Order order) {
        // domain -> entity -> (Spring Data JPA) -> entity -> domain
        OrderEntity entity = OrderEntityMapper.toEntity(order);
        OrderEntity savedEntity = jpaRepository.save(entity);
        return OrderEntityMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Order> findById(OrderId orderId) {
        return jpaRepository.findById(orderId.value()).map(OrderEntityMapper::toDomain);
    }

    @Override
    public OrderId nextOrderId() {
        // That ra day la trach nhiem cua ha tang: co the la sequence cua DB,
        // UUID, hay bo sinh ma rieng. Use case chi can biet no nhan duoc mot id.
        return OrderId.of("ORD-" + sequence.incrementAndGet());
    }
}
