package com.example.serviceonec.repository.invoice;

import com.example.serviceonec.model.entity.invoice.InvoiceStocksEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InvoiceStocksRepository extends JpaRepository<InvoiceStocksEntity, String> {
}
