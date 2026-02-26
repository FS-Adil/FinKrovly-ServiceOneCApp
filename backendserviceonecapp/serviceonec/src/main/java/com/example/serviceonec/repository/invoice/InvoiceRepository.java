package com.example.serviceonec.repository.invoice;

import com.example.serviceonec.model.entity.invoice.InvoiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InvoiceRepository extends JpaRepository<InvoiceEntity, String> {
    @Query("SELECT i.refKey FROM InvoiceEntity i")
    List<UUID> findAllRefKeys();

    @Query("SELECT i.refKey FROM InvoiceEntity i WHERE i.operationType = :operationType")
    List<UUID> findAllRefKeysByOperationType(@Param("operationType") String operationType);
}
