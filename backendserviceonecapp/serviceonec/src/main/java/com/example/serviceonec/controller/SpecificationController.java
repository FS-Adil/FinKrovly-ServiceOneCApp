package com.example.serviceonec.controller;

import com.example.serviceonec.model.entity.NomenclatureEntity;
import com.example.serviceonec.model.entity.specification.SpecificationEntity;
import com.example.serviceonec.service.NomenclatureService;
import com.example.serviceonec.service.specification.SpecificationService;
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
@RequestMapping("/api/v1/specification")
public class SpecificationController {

    private final SpecificationService specificationService;

    @GetMapping("/save-all-specification")
    public ResponseEntity<Page<SpecificationEntity>> getAllSpecification() {

        Page<SpecificationEntity> specificationEntities = specificationService.getAllSpecification();

        if (specificationEntities.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "not found");
        }
        return ResponseEntity.ok(specificationEntities);
    }
}
