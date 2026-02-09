package com.example.serviceonec.controller;

import com.example.serviceonec.model.entity.NomenclatureEntity;
import com.example.serviceonec.model.entity.StructuralUnitEntity;
import com.example.serviceonec.service.StructuralUnitService;
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
@RequestMapping("/api/v1/structural-unit")
public class StructuralUnitController {

    private final StructuralUnitService structuralUnitService;

    @GetMapping("/save-all-structural-unit")
    public ResponseEntity<Page<StructuralUnitEntity>> getAllNomenclature() {

        Page<StructuralUnitEntity> structuralUnitEntities = structuralUnitService.getAllStructuralUnit();

        if (structuralUnitEntities.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "not found");
        }
        return ResponseEntity.ok(structuralUnitEntities);
    }

}
