package com.example.serviceonec.controller.costprice;


import com.example.serviceonec.controller.assembly.input.AssemblyExpendControllerInput;
import com.example.serviceonec.controller.assembly.output.AssemblyExpendControllerOutput;
import com.example.serviceonec.controller.costprice.output.CostPriceControllerOutput;
import com.example.serviceonec.service.costprice.CostPriceService;
import com.example.serviceonec.service.expend.ExpendService;
import com.example.serviceonec.service.invoice.InvoiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v2/cost-price")
public class CostPriceController {

    private final CostPriceService costPriceService;
    private final ExpendService expendService;
    private final InvoiceService invoiceService;

    @PostMapping("/assembly")
    public ResponseEntity<List<CostPriceControllerOutput>> getAllAssemblyExpendCostPrice(
            @Valid @RequestBody AssemblyExpendControllerInput request
    ) {

        expendService.getAllExpend(
                request.getOrganizationId(),
                request.getDateFrom(),
                request.getDateTo()
        );

        invoiceService.getAllInvoice(
                request.getOrganizationId()
        );

        List<CostPriceControllerOutput> list = costPriceService.getAllCostPrice();

        if (list.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    String.format("Нет данных Организаций по данному UUID - %s", request.getOrganizationId().toString())
            );
        }

        return ResponseEntity.ok(list);
    }

}
