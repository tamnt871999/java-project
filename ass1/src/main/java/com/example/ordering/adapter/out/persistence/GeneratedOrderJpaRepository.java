package com.example.ordering.adapter.out.persistence;

import com.example.ordering.adapter.lib.Database;
import com.example.ordering.adapter.lib.SimpleJpaRepository;

/**
 * Dong vai PROXY MA SPRING DATA TU SINH luc chay.
 *
 * Trong Spring Boot ban khong viet file nay: framework doc interface
 * OrderJpaRepository roi sinh ban hien thuc bang proxy dong. Vi bai tap khong
 * dung Spring nen ta noi tay interface do vao SimpleJpaRepository cong so do
 * anh xa ORDER_MAPPING.
 *
 * Class nay biet OrderEntity nen nam o adapter/out/persistence; con
 * SimpleJpaRepository ben adapter/lib thi hoan toan tong quat, khong biet
 * Order la gi.
 */
public class GeneratedOrderJpaRepository
        extends SimpleJpaRepository<OrderEntity, String>
        implements OrderJpaRepository {

    public GeneratedOrderJpaRepository(Database database) {
        super(database, OrderPersistenceMapping.ORDER_MAPPING);
    }
}
