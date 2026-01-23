package com.example.serviceonec.controller;

import com.example.serviceonec.model.entity.InvoiceEntity;
import com.example.serviceonec.model.entity.NomenclatureEntity;
import com.example.serviceonec.service.NomenclatureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
public class NomenclatureController {

    private final NomenclatureService nomenclatureService;

    @GetMapping("/get-all-nomenclature")
    public ResponseEntity<List<NomenclatureEntity>> getAllNomenclature() {

        List<NomenclatureEntity> nomenclatureEntities = nomenclatureService.getAllNomenclature();

        if (nomenclatureEntities.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "not found");
        }
        return ResponseEntity.ok(nomenclatureEntities);
    }
}
