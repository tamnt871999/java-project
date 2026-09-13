package com.example.ordering.domain;

import java.util.Objects;

/** MODEL - khach hang. */
public class Customer {

    private final String id;
    private final String name;
    private final String email;
    private final boolean active;

    public Customer(String id, String name, String email, boolean active) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.email = Objects.requireNonNull(email, "email must not be null");
        this.active = active;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public boolean isActive() {
        return active;
    }

    @Override
    public String toString() {
        return name + " (" + id + ")";
    }
}
