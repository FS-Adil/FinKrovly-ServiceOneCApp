package com.example.serviceonec.repository.production;

import com.example.serviceonec.model.entity.production.ProductionStocksEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductionStocksRepository extends JpaRepository<ProductionStocksEntity, String> {
}
