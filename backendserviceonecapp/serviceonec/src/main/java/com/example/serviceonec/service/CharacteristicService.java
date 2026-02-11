package com.example.serviceonec.service;

import com.example.serviceonec.model.entity.CharacteristicEntity;
import org.springframework.data.domain.Page;

public interface CharacteristicService {
    Page<CharacteristicEntity> getAllCharacteristic();
}
