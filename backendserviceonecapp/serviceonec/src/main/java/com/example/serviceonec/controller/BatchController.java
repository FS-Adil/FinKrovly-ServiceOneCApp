package com.example.serviceonec.controller;

import com.example.serviceonec.model.entity.BatchEntity;
import com.example.serviceonec.model.entity.NomenclatureEntity;
import com.example.serviceonec.service.BatchService;
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
@RequestMapping("/api/v1/batch")
public class BatchController {

    private final BatchService batchService;

    @GetMapping("/save-all-batch")
    public ResponseEntity<Page<BatchEntity>> getAllBatch() {

        Page<BatchEntity> batchEntities = batchService.getAllBatch();

        if (batchEntities.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "not found");
        }
        return ResponseEntity.ok(batchEntities);
    }
}
