package com.example.serviceonec.service.expend;

import com.example.serviceonec.model.entity.expend.ExpendStocksEntity;
import com.example.serviceonec.model.entity.invoice.InvoiceStocksEntity;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ExpendStocksService {
    Page<ExpendStocksEntity> getAllExpendStocks();
}
