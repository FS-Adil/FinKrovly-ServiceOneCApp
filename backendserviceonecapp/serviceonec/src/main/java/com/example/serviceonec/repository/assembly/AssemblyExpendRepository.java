package com.example.serviceonec.repository.assembly;

import com.example.serviceonec.model.entity.assembly.AssemblyExpendEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AssemblyExpendRepository extends JpaRepository<AssemblyExpendEntity, String> {
}
