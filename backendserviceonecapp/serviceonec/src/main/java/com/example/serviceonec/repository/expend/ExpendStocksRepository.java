package com.example.serviceonec.repository.expend;

import com.example.serviceonec.model.entity.expend.ExpendStocksEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ExpendStocksRepository extends JpaRepository<ExpendStocksEntity, String> {
    List<ExpendStocksEntity> findAllByRefKey(UUID refKey);
}
