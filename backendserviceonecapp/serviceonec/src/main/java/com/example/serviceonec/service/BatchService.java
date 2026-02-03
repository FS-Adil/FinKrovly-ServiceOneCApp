package com.example.serviceonec.service;

import com.example.serviceonec.model.entity.BatchEntity;
import org.springframework.data.domain.Page;

public interface BatchService {
    Page<BatchEntity> getAllBatch();
}
