package com.example.serviceonec.controller;

import com.example.serviceonec.model.dto.response.InvoiceResponseDto;
import com.example.serviceonec.model.entity.InvoiceEntity;
import com.example.serviceonec.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/invoice")
public class InvoiceController {

    private final InvoiceService invoiceService;

    @GetMapping("/get-all-invoice")
    public ResponseEntity<List<InvoiceEntity>> getAllInvoice() {
//        LocalDateTime startDate = LocalDateTime.of(2025, 1, 1, 0, 0, 0);
//        LocalDateTime endDate = LocalDateTime.of(2025, 12, 31, 23, 59, 59);

//        Optional<InvoiceResponseDto> invoiceResponseDtoOptional = Optional.ofNullable(invoiceService.getAllInvoice(startDate, endDate));
        List<InvoiceEntity> invoiceEntities = invoiceService.getAllInvoice();

        if (invoiceEntities.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "not found");
        }
        return ResponseEntity.ok(invoiceEntities);
    }

}
