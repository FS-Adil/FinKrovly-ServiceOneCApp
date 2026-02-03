package com.example.serviceonec.service.assembly_expend;

import com.example.serviceonec.model.entity.assembly.AssemblyExpendEntity;
import org.springframework.data.domain.Page;

public interface AssemblyExpendService {
    Page<AssemblyExpendEntity> addAllAssemblyExpend();
    Page<AssemblyExpendEntity> costCalculation();
}
