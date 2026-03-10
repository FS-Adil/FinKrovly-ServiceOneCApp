package com.example.serviceonec.repository.specification;

import com.example.serviceonec.model.entity.specification.SpecificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpecificationRepository extends JpaRepository<SpecificationEntity, String> {
}
