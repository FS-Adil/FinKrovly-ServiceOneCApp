package com.example.serviceonec.repository.inventory;

import com.example.serviceonec.model.entity.inventory.InventoryStocksEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InventoryStocksRepository extends JpaRepository<InventoryStocksEntity, String> {
}
