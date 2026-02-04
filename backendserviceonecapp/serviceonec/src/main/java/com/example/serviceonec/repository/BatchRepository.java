package com.example.serviceonec.repository;

import com.example.serviceonec.model.entity.BatchEntity;
import com.example.serviceonec.model.entity.NomenclatureEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface BatchRepository extends JpaRepository<BatchEntity, String> {
    BatchEntity findByRefKey(UUID refKey);
}
