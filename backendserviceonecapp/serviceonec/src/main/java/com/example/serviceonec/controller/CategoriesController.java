package com.example.serviceonec.controller;

import com.example.serviceonec.model.dto.InputApiResponse;
import com.example.serviceonec.model.dto.response.CategoriesInputResponseDto;
import com.example.serviceonec.model.entity.NomenclatureEntity;
import com.example.serviceonec.service.CategoriesService;
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
@RequestMapping("/api/v1/categories")
@Tag(name = "Categories Item", description = "API для управления справочными эелементами категории материалов (description, refKey)")
public class CategoriesController {

    private final CategoriesService categoriesService;

    @GetMapping("/save-all-categories")
    @Operation(summary = "Получить список всех категорий материалов без пагинации")
    public ResponseEntity<InputApiResponse<List<CategoriesInputResponseDto>>> getAllCategories() {

        List<CategoriesInputResponseDto> categories = categoriesService.getAllCategories();

        return ResponseEntity.ok(InputApiResponse.success(categories));
    }

}
