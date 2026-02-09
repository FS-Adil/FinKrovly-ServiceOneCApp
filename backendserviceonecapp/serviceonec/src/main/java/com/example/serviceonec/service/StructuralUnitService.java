package com.example.serviceonec.service;

import com.example.serviceonec.model.entity.StructuralUnitEntity;
import org.springframework.data.domain.Page;

public interface StructuralUnitService {
    Page<StructuralUnitEntity> getAllStructuralUnit();
}
