package com.example.leaderboard.repository;

import com.example.leaderboard.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
