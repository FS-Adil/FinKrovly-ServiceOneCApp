package com.example.serviceonec.repository.remaining;

import com.example.serviceonec.model.entity.remaining.RemainingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RemainingRepository extends JpaRepository<RemainingEntity, String> {
}
