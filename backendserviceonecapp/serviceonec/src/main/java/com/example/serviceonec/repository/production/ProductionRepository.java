package com.example.serviceonec.repository.production;

import com.example.serviceonec.model.entity.production.ProductionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductionRepository extends JpaRepository<ProductionEntity, String> {
}
