package com.example.ordering.application;

import com.example.ordering.domain.Product;

import java.util.List;
import java.util.Objects;

/** SERVICE - nghiep vu lien quan toi catalog san pham. */
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = Objects.requireNonNull(productRepository);
    }

    public List<Product> findAll() {
        return productRepository.findAll();
    }

    public Product findById(String id) {
        return productRepository.findById(id)
                .orElseThrow(() -> ServiceException.notFound("Khong tim thay san pham: " + id));
    }
}
