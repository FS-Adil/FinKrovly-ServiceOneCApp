package com.example.serviceonec.repository.invoice;

import com.example.serviceonec.model.entity.invoice.InvoiceFullEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InvoiceFullRepository extends JpaRepository<InvoiceFullEntity, String> {
}
