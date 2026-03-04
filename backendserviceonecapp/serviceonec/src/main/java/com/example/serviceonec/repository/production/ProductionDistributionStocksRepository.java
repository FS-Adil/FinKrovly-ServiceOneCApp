package com.example.serviceonec.repository.production;

import com.example.serviceonec.model.entity.production.ProductionDistributionStocksEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductionDistributionStocksRepository extends JpaRepository<ProductionDistributionStocksEntity, String> {
}
