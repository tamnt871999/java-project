package com.example.ordering.infrastructure.db;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * FRAMEWORKS AND DRIVERS - dong vai lifeline "H2/Postgres (DB)".
 *
 * Day la CSDL gia lap trong bo nho, co in ra cau lenh SQL tuong duong de ban
 * nhin thay dung nhung gi mot database that se nhan duoc.
 *
 * Vi sao khong dung H2 that? Bai tap nay chay chi voi JDK 21, khong tai thu
 * vien ngoai. Doi lai, RANH GIOI van dung cho: chi co package infrastructure
 * biet class nay ton tai. Muon doi sang H2 that thi thay class nay bang mot
 * DataSource JDBC, khong file nao trong domain hay application phai sua.
 */
public class Database {

    private final String engineName;
    private final boolean logSql;
    /** tenBang -> (khoaChinh -> dong du lieu) */
    private final Map<String, Map<String, Map<String, Object>>> tables = new LinkedHashMap<>();

    public Database(String engineName, boolean logSql) {
        this.engineName = engineName;
        this.logSql = logSql;
    }

    public void insert(String table, String primaryKey, Map<String, Object> row) {
        tables.computeIfAbsent(table, name -> new LinkedHashMap<>())
                .put(primaryKey, new LinkedHashMap<>(row));
        log("INSERT INTO %s (%s) VALUES (%s)".formatted(
                table, String.join(", ", row.keySet()), placeholders(row)));
    }

    public Optional<Map<String, Object>> selectById(String table, String primaryKey) {
        log("SELECT * FROM %s WHERE id = '%s'".formatted(table, primaryKey));
        Map<String, Map<String, Object>> rows = tables.get(table);
        if (rows == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(rows.get(primaryKey)).map(LinkedHashMap::new);
    }

    public List<Map<String, Object>> selectWhere(String table, String column, Object value) {
        log("SELECT * FROM %s WHERE %s = '%s'".formatted(table, column, value));
        Map<String, Map<String, Object>> rows = tables.get(table);
        if (rows == null) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows.values()) {
            if (value.equals(row.get(column))) {
                result.add(new LinkedHashMap<>(row));
            }
        }
        return result;
    }

    public void deleteWhere(String table, String column, Object value) {
        Map<String, Map<String, Object>> rows = tables.get(table);
        if (rows == null || rows.isEmpty()) {
            return;
        }
        boolean removed = rows.entrySet().removeIf(entry -> value.equals(entry.getValue().get(column)));
        if (removed) {
            log("DELETE FROM %s WHERE %s = '%s'".formatted(table, column, value));
        }
    }

    private static String placeholders(Map<String, Object> row) {
        List<String> values = new ArrayList<>(row.size());
        for (Object value : row.values()) {
            values.add(value instanceof Number ? String.valueOf(value) : "'" + value + "'");
        }
        return String.join(", ", values);
    }

    private void log(String sql) {
        if (logSql) {
            System.out.println("    [" + engineName + "] " + sql);
        }
    }
}
