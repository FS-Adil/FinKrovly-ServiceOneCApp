package com.example.serviceonec.service.expend;

import com.example.serviceonec.model.entity.expend.ExpendEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ExpendService {
    List<ExpendEntity> getAllExpend(UUID organizationId, LocalDateTime startDate, LocalDateTime endDate);
}
