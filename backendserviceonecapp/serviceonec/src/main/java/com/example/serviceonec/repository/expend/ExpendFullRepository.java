package com.example.serviceonec.repository.expend;

import com.example.serviceonec.model.entity.expend.ExpendFullEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExpendFullRepository extends JpaRepository<ExpendFullEntity, String> {
}
