package com.example.ordering.application;

import com.example.ordering.domain.Customer;

import java.util.List;
import java.util.Optional;

/** Repository - truy cap du lieu khach hang. */
public interface CustomerRepository {

    Optional<Customer> findById(String id);

    List<Customer> findAll();
}
