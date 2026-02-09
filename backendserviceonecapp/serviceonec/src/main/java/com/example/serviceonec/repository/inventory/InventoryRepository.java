package com.example.serviceonec.repository.inventory;

import com.example.serviceonec.model.entity.inventory.InventoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InventoryRepository extends JpaRepository<InventoryEntity, String> {
}
