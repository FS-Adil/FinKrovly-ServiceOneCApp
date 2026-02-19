package com.example.serviceonec.service.assembly_expend;

import com.example.serviceonec.controller.assembly.output.AssemblyExpendControllerOutput;
import com.example.serviceonec.model.entity.assembly.AssemblyExpendEntity;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface AssemblyExpendService {
    Page<AssemblyExpendEntity> addAllAssemblyExpend();
    Page<AssemblyExpendEntity> costCalculation();
    List<AssemblyExpendControllerOutput> findAllExpendCost(UUID organizationId,
                                                           LocalDateTime dateFrom,
                                                           LocalDateTime dateTo);
}
