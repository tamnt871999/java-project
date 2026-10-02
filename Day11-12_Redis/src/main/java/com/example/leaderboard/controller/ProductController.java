package com.example.leaderboard.controller;

import com.example.leaderboard.service.ProductFacade;
import com.example.leaderboard.service.ProductService.ProductView;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductFacade products;

    public ProductController(ProductFacade products) {
        this.products = products;
    }

    @GetMapping("/{id}")
    public ProductView findById(@PathVariable("id") Long id) {
        return products.findById(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") Long id) {
        products.delete(id);
    }
}
