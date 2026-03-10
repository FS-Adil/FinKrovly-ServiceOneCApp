package com.example.serviceonec.service.specification;

import com.example.serviceonec.model.entity.NomenclatureEntity;
import com.example.serviceonec.model.entity.specification.SpecificationEntity;
import org.springframework.data.domain.Page;

public interface SpecificationService {
    Page<SpecificationEntity> getAllSpecification();
}
