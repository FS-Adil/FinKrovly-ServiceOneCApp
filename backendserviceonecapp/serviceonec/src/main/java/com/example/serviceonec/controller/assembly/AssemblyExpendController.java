package com.example.serviceonec.controller.assembly;

import com.example.serviceonec.model.entity.assembly.AssemblyExpendEntity;
import com.example.serviceonec.model.entity.expend.ExpendStocksEntity;
import com.example.serviceonec.service.assembly_expend.AssemblyExpendService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/assembly")
public class AssemblyExpendController {

    private final AssemblyExpendService assemblyExpendService;

    @GetMapping("/create-assembly-expends")
    public ResponseEntity<Page<AssemblyExpendEntity>> getAllAssembleExpend() {
//        LocalDateTime startDate = LocalDateTime.of(2025, 1, 1, 0, 0, 0);
//        LocalDateTime endDate = LocalDateTime.of(2025, 12, 31, 23, 59, 59);

//        Optional<InvoiceResponseDto> invoiceResponseDtoOptional = Optional.ofNullable(invoiceService.getAllInvoice(startDate, endDate));
        Page<AssemblyExpendEntity> assemblyExpendEntities   = assemblyExpendService.addAllAssemblyExpend();

        if (assemblyExpendEntities.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "not found");
        }
        return ResponseEntity.ok(assemblyExpendEntities);
    }

    @GetMapping("/add-cost-price-assembly-expends")
    public ResponseEntity<Page<AssemblyExpendEntity>> addAssemblyExpendCostPrice() {
//        LocalDateTime startDate = LocalDateTime.of(2025, 1, 1, 0, 0, 0);
//        LocalDateTime endDate = LocalDateTime.of(2025, 12, 31, 23, 59, 59);

//        Optional<InvoiceResponseDto> invoiceResponseDtoOptional = Optional.ofNullable(invoiceService.getAllInvoice(startDate, endDate));
        Page<AssemblyExpendEntity> assemblyExpendEntities   = assemblyExpendService.costCalculation();

        if (assemblyExpendEntities.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "not found");
        }
        return ResponseEntity.ok(assemblyExpendEntities);
    }
}
