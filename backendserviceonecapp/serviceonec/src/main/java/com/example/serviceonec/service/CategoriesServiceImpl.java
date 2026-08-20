package com.example.serviceonec.service;

import com.example.serviceonec.config.RestClientConfig;
import com.example.serviceonec.model.dto.response.CategoriesInputResponseDto;
import com.example.serviceonec.model.dto.response.CategoriesItemOutputResponseDto;
import com.example.serviceonec.model.dto.response.CategoriesOutputResponseDto;
import com.example.serviceonec.model.entity.CategoriesEntity;
import com.example.serviceonec.repository.CategoriesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class CategoriesServiceImpl implements CategoriesService {

    private final CategoriesRepository categoriesRepository;

    private final RestClientConfig restClientConfig;

    @Override
    public List<CategoriesInputResponseDto> getAllCategories() {
        log.info("--> Вносим изменения в таблицу categories.");
        categoriesRepository.deleteAll();

        List<CategoriesEntity> entities = new ArrayList<>();

        for (CategoriesItemOutputResponseDto value : getCategories().getValue()) {
            entities.add(
                    CategoriesEntity.builder()
                            .description(value.getDescription())
                            .refKey(value.getRefKey())
                            .build()
            );
        }

        saveEntitiesIndividually(entities);

        Page<CategoriesEntity> page = categoriesRepository.findAll(PageRequest.of(0, 10));

        return page.getContent().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private void saveEntitiesIndividually(List<CategoriesEntity> entities) {
        log.info("Пытаемся сохранить {} записей по одной", entities.size());
        int successCount = 0;

        for (CategoriesEntity entity : entities) {
            try {
                categoriesRepository.save(entity);
                successCount++;
            } catch (Exception e) {
                log.error("Не удалось сохранить категорию: {}", entity.getDescription(), e);
            }
        }

        log.info("Успешно сохранено {} из {} записей", successCount, entities.size());
    }

    private CategoriesOutputResponseDto getCategories() {
        String url = "/Catalog_КатегорииНоменклатуры?" +
                "$select=Description,Ref_Key&" +
                "$format=json";

        try {
            return restClientConfig.restClient().get()
                    .uri(url)
                    .retrieve()
                    .body(CategoriesOutputResponseDto.class);
        } catch (Exception e) {
            log.error("Ошибка при получении Категорий номенклатуры",  e);
            throw new RuntimeException("Ошибка получения данных из 1С", e);
        }
    }

    private CategoriesInputResponseDto toResponse(CategoriesEntity entity) {
        return CategoriesInputResponseDto.builder()
                .description(entity.getDescription())
                .refKey(entity.getRefKey().toString())
                .build();
    }
}
