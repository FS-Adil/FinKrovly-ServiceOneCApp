package com.example.serviceonec.controller.invoice;

import com.example.serviceonec.model.entity.invoice.InvoiceStocksEntity;
import com.example.serviceonec.service.invoice.InvoiceStocksService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/invoice-stocks")
public class InvoiceStocksController {

    private final InvoiceStocksService invoiceStocksService;

    @GetMapping("/save-all-invoice-stocks")
    public ResponseEntity<Page<InvoiceStocksEntity>> getAllInvoiceStocks() {
//        LocalDateTime startDate = LocalDateTime.of(2025, 1, 1, 0, 0, 0);
//        LocalDateTime endDate = LocalDateTime.of(2025, 12, 31, 23, 59, 59);

//        Optional<InvoiceResponseDto> invoiceResponseDtoOptional = Optional.ofNullable(invoiceService.getAllInvoice(startDate, endDate));
        Page<InvoiceStocksEntity> invoiceStocksEntities = invoiceStocksService.getAllInvoiceStocks();

        if (invoiceStocksEntities.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "not found");
        }
        return ResponseEntity.ok(invoiceStocksEntities);
    }
}
