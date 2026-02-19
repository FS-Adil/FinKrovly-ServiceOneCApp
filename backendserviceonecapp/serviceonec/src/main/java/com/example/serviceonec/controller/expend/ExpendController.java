package com.example.serviceonec.controller.expend;

import com.example.serviceonec.model.entity.expend.ExpendEntity;
import com.example.serviceonec.service.expend.ExpendService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/expend")
public class ExpendController {

    private final ExpendService expendService;

    @GetMapping("/save-all-expend")
    public ResponseEntity<Page<ExpendEntity>> getAllInvoice() {
        LocalDateTime startDate = LocalDateTime.of(2025, 12, 1, 0, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(2025, 12, 31, 23, 59, 59);

//        Optional<InvoiceResponseDto> invoiceResponseDtoOptional = Optional.ofNullable(invoiceService.getAllInvoice(startDate, endDate));
        UUID randomUUID = UUID.randomUUID();
        Page<ExpendEntity> expendEntities = expendService.getAllExpend(
                randomUUID,
                startDate,
                endDate
        );

        if (expendEntities.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    String.format("Нет данных по данному UUID - %s", randomUUID)
            );
        }
        return ResponseEntity.ok(expendEntities);
    }
}
