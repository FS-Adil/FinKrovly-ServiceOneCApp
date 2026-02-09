package com.example.serviceonec.repository;

import com.example.serviceonec.model.entity.CharacteristicEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CharacteristicRepository extends JpaRepository<CharacteristicEntity, String> {
    CharacteristicEntity findByRefKey(UUID refKey);
}
