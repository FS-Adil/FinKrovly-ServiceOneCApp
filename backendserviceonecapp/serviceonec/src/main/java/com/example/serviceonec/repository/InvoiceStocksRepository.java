package com.example.serviceonec.repository;

import com.example.serviceonec.model.entity.InvoiceStocksEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InvoiceStocksRepository extends JpaRepository<InvoiceStocksEntity, String> {
}
