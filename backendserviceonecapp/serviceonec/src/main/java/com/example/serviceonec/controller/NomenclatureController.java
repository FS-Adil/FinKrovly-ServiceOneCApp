package com.example.serviceonec.controller;

import com.example.serviceonec.model.entity.NomenclatureEntity;
import com.example.serviceonec.service.NomenclatureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping("/api/v1/nomenclature")
@Tag(name = "Nomenclature Item", description = "API для управления справочными эелементами номенклатура")
public class NomenclatureController {

    private final NomenclatureService nomenclatureService;

    @GetMapping("/save-all-nomenclature")
    @Operation(summary = "Получить список всех номенклатур без пагинации")
    public ResponseEntity<Page<NomenclatureEntity>> getAllNomenclature() {

        Page<NomenclatureEntity> nomenclatureEntities = nomenclatureService.getAllNomenclature();

        if (nomenclatureEntities.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "not found");
        }
        return ResponseEntity.ok(nomenclatureEntities);
    }
}
