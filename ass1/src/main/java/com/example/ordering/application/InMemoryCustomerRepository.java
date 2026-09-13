package com.example.ordering.application;

import com.example.ordering.domain.Customer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Ban hien thuc bang HashMap. */
public class InMemoryCustomerRepository implements CustomerRepository {

    private final Map<String, Customer> data = new LinkedHashMap<>();

    public InMemoryCustomerRepository() {
        add(new Customer("CUS-001", "Nguyen Van A", "a@example.com", true));
        add(new Customer("CUS-002", "Tran Thi B", "b@example.com", true));
        add(new Customer("CUS-BLOCKED", "Le Van C (bi khoa)", "c@example.com", false));
    }

    private void add(Customer customer) {
        data.put(customer.getId(), customer);
    }

    @Override
    public Optional<Customer> findById(String id) {
        return Optional.ofNullable(data.get(id));
    }

    @Override
    public List<Customer> findAll() {
        return new ArrayList<>(data.values());
    }
}
