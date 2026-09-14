package com.example.ordering.infrastructure.jpa;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Mo ta cach mot entity duoc trai ra thanh cac dong trong bang.
 *
 * Trong Spring Data that, thong tin nay duoc doc tu annotation @Entity,
 * @Table, @Column, @OneToMany bang reflection. O day ta khai bao tuong minh
 * bang cac ham - de nhin thay ro rang framework thuc su dang lam gi.
 *
 * @param table      bang cha, vi du "orders"
 * @param childTable bang con, vi du "order_items"; null neu entity khong co bang con
 * @param foreignKey ten cot khoa ngoai o bang con, vi du "order_id"
 */
public record EntityMapping<T, ID>(
        String table,
        String childTable,
        String foreignKey,
        Function<T, ID> idOf,
        Function<T, Map<String, Object>> toRow,
        Function<T, List<Map<String, Object>>> toChildRows,
        BiFunction<Map<String, Object>, List<Map<String, Object>>, T> fromRows) {

    public boolean hasChildTable() {
        return childTable != null;
    }
}
