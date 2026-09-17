package com.example.ordering.adapter.out.persistence;

import com.example.ordering.adapter.lib.JpaRepository;

/**
 * Khai bao repository theo dung kieu Spring Data.
 *
 * Trong Spring Boot ban chi viet dung mot dong nay va KHONG viet ban hien
 * thuc - framework tu sinh proxy luc chay:
 *
 *     public interface OrderJpaRepository extends JpaRepository<OrderEntity, String> { }
 *
 * O bai nay ban hien thuc la SimpleJpaRepository ben package adapter/lib,
 * duoc lap rap trong Main.
 */
public interface OrderJpaRepository extends JpaRepository<OrderEntity, String> {
}
