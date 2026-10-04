package com.example.inventory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Version;
import org.hibernate.envers.Audited;

@Entity
@Audited
public class Inventory {

    @Id
    private Long productId;

    @Column(nullable = false)
    private String productName;

    @Audited(withModifiedFlag = true)
    @Column(nullable = false)
    private int quantity;

    @Version
    private long version;

    protected Inventory() {
    }

    public Inventory(Long productId, String productName, int quantity) {
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
    }

    public void stockIn(int amount) {
        quantity += amount;
    }

    public void rename(String productName) {
        this.productName = productName;
    }

    public Long getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public int getQuantity() {
        return quantity;
    }

    public long getVersion() {
        return version;
    }
}
