package com.example.serviceonec.repository.invoice;

import com.example.serviceonec.model.entity.invoice.InvoiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InvoiceRepository extends JpaRepository<InvoiceEntity, String> {
}
