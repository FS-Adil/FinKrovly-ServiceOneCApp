package com.example.serviceonec.repository.production;

import com.example.serviceonec.model.entity.expend.ExpendStocksEntity;
import com.example.serviceonec.model.entity.production.ProductionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProductionRepository extends JpaRepository<ProductionEntity, String> {
    List<ProductionEntity> findAllByCustomerOrderKeyIn(List<UUID> docOrders);
}
