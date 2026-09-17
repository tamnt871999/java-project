package com.example.ordering.adapter.lib;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Mo ta cach mot entity duoc trai ra thanh cac dong trong bang VA cach dung
 * no lai tu cac dong do.
 *
 * Trong Spring Data that, thong tin nay duoc doc tu annotation @Entity,
 * @Table, @Column, @OneToMany bang reflection - va Hibernate dung chung mot bo
 * metadata cho ca hai chieu ghi va doc. O day ta khai bao tuong minh bang cac
 * ham, de nhin thay ro rang framework thuc su dang lam gi.
 *
 * @param table      bang cha, vi du "orders"
 * @param childTable bang con, vi du "order_items"; null neu entity khong co bang con
 * @param foreignKey ten cot khoa ngoai o bang con, vi du "order_id"
 * @param fromRows   chieu nguoc: dong cha + cac dong con -> entity
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
