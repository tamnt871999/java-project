package com.example.ordering.adapter;

import com.example.ordering.domain.Customer;
import com.example.ordering.domain.Product;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** REPRESENTATION cua Product va Customer. */
final class ProductJson {

    static Map<String, Object> toJson(Product product) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("productId", product.getId());
        json.put("name", product.getName());
        json.put("price", product.getPrice().amount());
        json.put("stock", product.getStock());
        json.put("inStock", product.isInStock());
        return json;
    }

    static List<Object> toJson(List<Product> products) {
        List<Object> result = new ArrayList<>();
        products.forEach(product -> result.add(toJson(product)));
        return result;
    }

    static Map<String, Object> toJson(Customer customer) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("customerId", customer.getId());
        json.put("name", customer.getName());
        json.put("email", customer.getEmail());
        json.put("active", customer.isActive());
        return json;
    }

    static List<Object> customersToJson(List<Customer> customers) {
        List<Object> result = new ArrayList<>();
        customers.forEach(customer -> result.add(toJson(customer)));
        return result;
    }

    private ProductJson() {
    }
}
