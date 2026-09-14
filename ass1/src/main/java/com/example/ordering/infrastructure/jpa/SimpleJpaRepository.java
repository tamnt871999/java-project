package com.example.ordering.infrastructure.jpa;

import com.example.ordering.infrastructure.db.Database;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * FRAMEWORKS AND DRIVERS - ban hien thuc cua JpaRepository.
 *
 * Spring Data JPA sinh class nay luc chay bang proxy dong; o day ta viet tay
 * de nhin ro no lam gi: nhan mot entity, tra thanh cac dong du lieu, roi ban
 * cau lenh SQL xuong database.
 *
 * Class nay van TONG QUAT - no chi biet EntityMapping va Database, khong biet
 * Order la gi. Do la ly do no nam duoc o vong ngoai cung ma khong lam ban
 * kien truc.
 */
public class SimpleJpaRepository<T, ID> implements JpaRepository<T, ID> {

    private final Database database;
    private final EntityMapping<T, ID> mapping;

    public SimpleJpaRepository(Database database, EntityMapping<T, ID> mapping) {
        this.database = Objects.requireNonNull(database, "database must not be null");
        this.mapping = Objects.requireNonNull(mapping, "mapping must not be null");
    }

    @Override
    public T save(T entity) {
        ID id = mapping.idOf().apply(entity);
        String primaryKey = String.valueOf(id);

        // Ghi bang cha.
        database.insert(mapping.table(), primaryKey, mapping.toRow().apply(entity));

        // Ghi bang con: xoa het roi ghi lai, dung cach JPA dong bo mot @OneToMany.
        if (mapping.hasChildTable()) {
            database.deleteWhere(mapping.childTable(), mapping.foreignKey(), primaryKey);
            List<Map<String, Object>> childRows = mapping.toChildRows().apply(entity);
            for (int i = 0; i < childRows.size(); i++) {
                database.insert(mapping.childTable(), primaryKey + "#" + i, childRows.get(i));
            }
        }

        return entity;
    }

}
