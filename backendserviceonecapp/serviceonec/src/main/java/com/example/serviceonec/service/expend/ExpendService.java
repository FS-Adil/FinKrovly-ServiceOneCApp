package com.example.serviceonec.service.expend;

import com.example.serviceonec.model.entity.expend.ExpendEntity;

import java.time.LocalDateTime;
import java.util.List;

public interface ExpendService {
    List<ExpendEntity> getAllExpend(LocalDateTime startDate, LocalDateTime endDate);
}
