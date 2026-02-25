package com.example.serviceonec.repository.invoice;

import com.example.serviceonec.model.entity.expend.ExpendStocksEntity;
import com.example.serviceonec.model.entity.invoice.InvoiceStocksEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InvoiceStocksRepository extends JpaRepository<InvoiceStocksEntity, String> {
    List<InvoiceStocksEntity> findAllByNomenclatureKey(UUID key);
    List<InvoiceStocksEntity> findAllByRefKey(UUID refKey);
}
