package com.example.ordering.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface OrderJpaRepository extends JpaRepository<OrderJpaEntity, Long> {
}
