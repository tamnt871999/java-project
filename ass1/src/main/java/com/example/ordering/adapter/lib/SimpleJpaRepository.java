package com.example.ordering.adapter.lib;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * THU VIEN GIA LAP - ban hien thuc cua JpaRepository.
 *
 * Spring Data JPA sinh class nay luc chay bang proxy dong; o day ta viet tay
 * de nhin ro no lam gi: nhan mot entity, tra thanh cac dong du lieu, roi ban
 * cau lenh SQL xuong database - va nguoc lai khi doc.
 *
 * Class nay van TONG QUAT - no chi biet EntityMapping va Database, khong biet
 * Order la gi. Co mot fitness function canh dieu do: khong file nao trong
 * adapter/lib duoc phep import bat ky package nao cua du an.
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

    /**
     * Doc entity theo khoa chinh.
     *
     * Hai cau SELECT lien tiep - mot cho bang cha, mot cho bang con - chinh la
     * hien tuong N+1 query ma ban gap khi nap @OneToMany o che do EAGER. Giu
     * nguyen o day de ban nhin thay no trong log SQL thay vi chi nghe ke.
     */
    @Override
    public Optional<T> findById(ID id) {
        String primaryKey = String.valueOf(id);

        Optional<Map<String, Object>> parentRow = database.selectById(mapping.table(), primaryKey);
        if (parentRow.isEmpty()) {
            return Optional.empty();
        }

        List<Map<String, Object>> childRows = mapping.hasChildTable()
                ? database.selectWhere(mapping.childTable(), mapping.foreignKey(), primaryKey)
                : List.of();

        return Optional.of(mapping.fromRows().apply(parentRow.get(), childRows));
    }
}
