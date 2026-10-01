package com.example.leaderboard.service;

import com.example.leaderboard.entity.Product;
import com.example.leaderboard.exception.ProductNotFoundException;
import com.example.leaderboard.repository.ProductRepository;
import java.io.Serializable;
import java.math.BigDecimal;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class ProductService {

    public static final String CACHE = "products";

    private final ProductRepository products;

    public ProductService(ProductRepository products) {
        this.products = products;
    }

    @Cacheable(CACHE)
    public ProductView findById(Long id) {
        return products.findById(id)
                .map(ProductView::from)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    @CacheEvict(CACHE)
    public void delete(Long id) {
        products.deleteById(id);
    }

    public record ProductView(Long id, String name, BigDecimal price) implements Serializable {

        static ProductView from(Product product) {
            return new ProductView(product.getId(), product.getName(), product.getPrice());
        }
    }
}
