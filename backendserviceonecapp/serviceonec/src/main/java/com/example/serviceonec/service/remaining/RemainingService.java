package com.example.serviceonec.service.remaining;

import com.example.serviceonec.model.entity.expend.ExpendEntity;
import com.example.serviceonec.model.entity.remaining.RemainingEntity;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.UUID;

public interface RemainingService {
    Page<RemainingEntity> getAllRemaining(UUID organizationId, LocalDateTime endDate);
}
