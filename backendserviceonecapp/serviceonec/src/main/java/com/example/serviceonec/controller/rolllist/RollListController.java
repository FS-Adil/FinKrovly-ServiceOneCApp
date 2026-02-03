package com.example.serviceonec.controller.rolllist;

import com.example.serviceonec.model.dto.response.rolllist.RollListItemResponseDto;
import com.example.serviceonec.model.entity.expend.ExpendEntity;
import com.example.serviceonec.model.entity.rolllist.RollListEntity;
import com.example.serviceonec.service.rolllist.RollListService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/roll-list")
public class RollListController {

    private final RollListService rollListService;

    @GetMapping("/find-all")
    public ResponseEntity<List<RollListEntity>> getAllRoll() {

        List<RollListEntity> rollListEntities = rollListService.getAllClosedRoll();

        if (rollListEntities.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "not found");
        }
        return ResponseEntity.ok(rollListEntities);
    }
}
