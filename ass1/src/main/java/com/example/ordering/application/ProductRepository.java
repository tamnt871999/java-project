package com.example.ordering.application;

import com.example.ordering.domain.Product;

import java.util.List;
import java.util.Optional;

/**
 * Repository - cua ngo truy cap du lieu san pham.
 *
 * Van tach interface de sau nay doi sang JDBC/JPA cho de, nhung khac voi
 * Hexagonal: o day interface va ban hien thuc nam CHUNG mot package, va
 * Service phu thuoc truc tiep vao no chu khong co khai niem "port".
 */
public interface ProductRepository {

    Optional<Product> findById(String id);

    List<Product> findAll();

    void save(Product product);
}
