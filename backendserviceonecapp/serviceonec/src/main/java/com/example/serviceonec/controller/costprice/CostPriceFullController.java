package com.example.serviceonec.controller.costprice;

import com.example.serviceonec.controller.assembly.input.AssemblyExpendControllerInput;
import com.example.serviceonec.controller.costprice.output.CostPriceControllerOutput;
import com.example.serviceonec.service.costprice.CostPriceFullService;
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
@RequestMapping("/api/v3/cost-price")
public class CostPriceFullController {

    private final CostPriceFullService costPriceFullService;

    @PostMapping("/assembly")
    public ResponseEntity<List<CostPriceControllerOutput>> getAllAssemblyExpendCostPrice(
            @Valid @RequestBody AssemblyExpendControllerInput request
    ) {

        List<CostPriceControllerOutput> list = costPriceFullService.getAllCostPrice(
                request.getOrganizationId(),
                request.getDateFrom(),
                request.getDateTo()
        );

        if (list.isEmpty()) {
            log.error("Нет данных Организаций по данному UUID - {}", request.getOrganizationId());
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    String.format("Нет данных Организаций по данному UUID - %s", request.getOrganizationId().toString())
            );
        }

        return ResponseEntity.ok(list);
    }
}
