package com.example.serviceonec.repository.expend;

import com.example.serviceonec.model.entity.expend.ExpendEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExpendRepository extends JpaRepository<ExpendEntity, String> {
}
