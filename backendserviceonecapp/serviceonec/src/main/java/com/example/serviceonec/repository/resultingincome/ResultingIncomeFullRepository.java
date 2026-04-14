package com.example.serviceonec.repository.resultingincome;

import com.example.serviceonec.model.entity.resultingincome.ResultingIncomeFullEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResultingIncomeFullRepository extends JpaRepository<ResultingIncomeFullEntity, String> {
    List<ResultingIncomeFullEntity> findAllByOrderByDateDesc();
}
