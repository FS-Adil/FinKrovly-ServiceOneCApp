package com.example.serviceonec.repository;

import com.example.serviceonec.model.entity.StructuralUnitEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StructuralUnitRepository extends JpaRepository<StructuralUnitEntity, String> {
}
