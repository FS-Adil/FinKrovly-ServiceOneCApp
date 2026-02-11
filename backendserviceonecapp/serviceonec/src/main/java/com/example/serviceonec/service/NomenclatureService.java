package com.example.serviceonec.service;

import com.example.serviceonec.model.entity.NomenclatureEntity;
import org.springframework.data.domain.Page;

public interface NomenclatureService {
    Page<NomenclatureEntity> getAllNomenclature();
}
