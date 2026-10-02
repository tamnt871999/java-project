package com.example.inventory.config;

import com.example.inventory.entity.Inventory;
import com.example.inventory.repository.InventoryRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class InventorySeeder implements ApplicationRunner {

    public static final long PRODUCT_ID = 1L;

    private final InventoryRepository inventories;

    public InventorySeeder(InventoryRepository inventories) {
        this.inventories = inventories;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!inventories.existsById(PRODUCT_ID)) {
            inventories.save(new Inventory(PRODUCT_ID, "Ban phim co", 100));
        }
    }
}
