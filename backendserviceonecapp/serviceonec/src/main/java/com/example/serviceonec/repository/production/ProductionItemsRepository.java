package com.example.serviceonec.repository.production;

import com.example.serviceonec.model.entity.production.ProductionItemsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductionItemsRepository extends JpaRepository<ProductionItemsEntity, String> {
}
