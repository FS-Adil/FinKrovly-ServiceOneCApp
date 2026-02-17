package com.example.serviceonec.controller.assembly;

import com.example.serviceonec.controller.assembly.input.AssemblyExpendControllerInput;
import com.example.serviceonec.controller.assembly.output.AssemblyExpendControllerOutput;
import com.example.serviceonec.model.entity.assembly.AssemblyExpendEntity;
import com.example.serviceonec.model.entity.expend.ExpendStocksEntity;
import com.example.serviceonec.service.assembly_expend.AssemblyExpendService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

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

    @PostMapping("/find-all")
    public ResponseEntity<List<AssemblyExpendControllerOutput>> getAllAssemblyExpendCostPrice(
            @Valid @RequestBody AssemblyExpendControllerInput request
    ) {

        List<AssemblyExpendControllerOutput> list = assemblyExpendService.findAllExpendCost(
                request.getOrganizationId(),
                request.getDateFrom(),
                request.getDateTo()
        );

        if (list.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    String.format("Нет данных по данному UUID - %s", request.getOrganizationId().toString())
            );
        }

        return ResponseEntity.ok(list);
    }
}
