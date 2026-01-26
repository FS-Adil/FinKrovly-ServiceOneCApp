package com.example.serviceonec.repository.expend;

import com.example.serviceonec.model.entity.expend.ExpendStocksEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExpendStocksRepository extends JpaRepository<ExpendStocksEntity, String> {
}
