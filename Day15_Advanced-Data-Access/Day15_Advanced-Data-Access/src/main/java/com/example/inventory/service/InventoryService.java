package com.example.inventory.service;

import com.example.inventory.entity.Inventory;
import com.example.inventory.exception.InventoryNotFoundException;
import com.example.inventory.exception.InventoryVersionConflictException;
import com.example.inventory.repository.InventoryRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.DefaultRevisionEntity;
import org.hibernate.envers.query.AuditEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {

    private final InventoryRepository inventories;
    private final EntityManager entityManager;

    public InventoryService(InventoryRepository inventories, EntityManager entityManager) {
        this.inventories = inventories;
        this.entityManager = entityManager;
    }

    @Transactional
    public InventoryView stockIn(Long productId, int amount) {
        Inventory inventory = inventories.findByIdForUpdate(productId)
                .orElseThrow(() -> new InventoryNotFoundException(productId));
        inventory.stockIn(amount);
        return InventoryView.from(inventories.saveAndFlush(inventory));
    }

    @Transactional
    public InventoryView update(Long productId, String productName, long expectedVersion) {
        Inventory inventory = inventories.findById(productId)
                .orElseThrow(() -> new InventoryNotFoundException(productId));
        if (inventory.getVersion() != expectedVersion) {
            throw new InventoryVersionConflictException(productId, expectedVersion, inventory.getVersion());
        }
        inventory.rename(productName);
        return InventoryView.from(inventories.saveAndFlush(inventory));
    }

    @Transactional(readOnly = true)
    public List<QuantityChange> quantityHistory(Long productId) {
        if (!inventories.existsById(productId)) {
            throw new InventoryNotFoundException(productId);
        }
        List<?> revisions = AuditReaderFactory.get(entityManager).createQuery()
                .forRevisionsOfEntity(Inventory.class, false, true)
                .add(AuditEntity.id().eq(productId))
                .add(AuditEntity.property("quantity").hasChanged())
                .addOrder(AuditEntity.revisionNumber().asc())
                .getResultList();
        return revisions.stream()
                .map(Object[].class::cast)
                .map(QuantityChange::from)
                .toList();
    }

    public record InventoryView(Long productId, String productName, int quantity, long version) {

        static InventoryView from(Inventory inventory) {
            return new InventoryView(inventory.getProductId(), inventory.getProductName(),
                    inventory.getQuantity(), inventory.getVersion());
        }
    }

    public record QuantityChange(int revision, Instant changedAt, int quantity) {

        static QuantityChange from(Object[] row) {
            Inventory snapshot = (Inventory) row[0];
            DefaultRevisionEntity revision = (DefaultRevisionEntity) row[1];
            return new QuantityChange(revision.getId(), Instant.ofEpochMilli(revision.getTimestamp()),
                    snapshot.getQuantity());
        }
    }
}
