package com.example.ordering.adapter;

import com.example.ordering.application.CustomerRepository;
import com.example.ordering.application.ProductService;
import com.example.ordering.application.ServiceException;

import java.util.Objects;

/**
 * REST CONTROLLER cho tai nguyen /api/products va /api/customers.
 *
 * Controller chi lam 3 viec: nhan tham so, goi Service, dong goi ket qua
 * thanh ApiResponse. Khong mot dong quy tac nghiep vu nao duoc nam o day.
 */
public class ProductController {

    private final ProductService productService;
    private final CustomerRepository customerRepository;

    public ProductController(ProductService productService, CustomerRepository customerRepository) {
        this.productService = Objects.requireNonNull(productService);
        this.customerRepository = Objects.requireNonNull(customerRepository);
    }

    /** GET /api/products */
    public ApiResponse list() {
        return ApiResponse.ok(ProductJson.toJson(productService.findAll()));
    }

    /** GET /api/products/{id} */
    public ApiResponse getOne(String productId) {
        try {
            return ApiResponse.ok(ProductJson.toJson(productService.findById(productId)));
        } catch (ServiceException notFound) {
            return ApiResponse.error(404, notFound.getCode(), notFound.getMessage(), notFound.getDetails());
        }
    }

    /** GET /api/customers */
    public ApiResponse listCustomers() {
        return ApiResponse.ok(ProductJson.customersToJson(customerRepository.findAll()));
    }
}
