package com.example.serviceonec.controller;

import com.example.serviceonec.model.entity.CharacteristicEntity;
import com.example.serviceonec.service.CharacteristicService;
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
@RequestMapping("/api/v1/characteristic")
public class CharacteristicController {

    private final CharacteristicService characteristicService;

    @GetMapping("/save-all-characteristic")
    public ResponseEntity<Page<CharacteristicEntity>> getAllInvoice() {

        Page<CharacteristicEntity> characteristicEntities = characteristicService.getAllCharacteristic();

        if (characteristicEntities.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "not found");
        }
        return ResponseEntity.ok(characteristicEntities);
    }
}
