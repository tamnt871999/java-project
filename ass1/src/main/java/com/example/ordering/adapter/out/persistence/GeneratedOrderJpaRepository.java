package com.example.ordering.adapter.out.persistence;

import com.example.ordering.infrastructure.db.Database;
import com.example.ordering.infrastructure.jpa.SimpleJpaRepository;

/**
 * Dong vai PROXY MA SPRING DATA TU SINH luc chay.
 *
 * Trong Spring Boot ban khong viet file nay: framework doc interface
 * OrderJpaRepository roi sinh ban hien thuc bang proxy dong. Vi bai tap khong
 * dung Spring nen ta noi tay interface do vao SimpleJpaRepository cong so do
 * anh xa ORDER_MAPPING.
 *
 * Class nay dat o vong Interface Adapters chu khong phai Frameworks, vi no
 * gan chat voi OrderEntity cua ta; con SimpleJpaRepository ben infrastructure
 * thi hoan toan tong quat, khong biet Order la gi.
 */
public class GeneratedOrderJpaRepository
        extends SimpleJpaRepository<OrderEntity, String>
        implements OrderJpaRepository {

    public GeneratedOrderJpaRepository(Database database) {
        super(database, OrderPersistenceMapping.ORDER_MAPPING);
    }
}
