package com.example.serviceonec.service.remaining;

import com.example.serviceonec.config.RestClientConfig;
import com.example.serviceonec.model.dto.response.remaining.RemainingItemStockResponseDto;
import com.example.serviceonec.model.dto.response.remaining.RemainingStockResponseDto;
import com.example.serviceonec.model.entity.remaining.RemainingEntity;
import com.example.serviceonec.repository.remaining.RemainingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;


@Service
@Slf4j
@RequiredArgsConstructor
public class RemainingServiceImpl implements RemainingService{

    private final RestClientConfig restClientConfig;
    private final RemainingRepository remainingRepository;

    @Override
    public Page<RemainingEntity> getAllRemaining(UUID organizationId, LocalDateTime endDate) {
        log.info("===== НАЧАЛО ЗАГРУЗКИ ОТАТКОВ =====");
        log.info("Организация ID: {}", organizationId);
        log.info("Период: на - {}",
                endDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        long startTime = System.currentTimeMillis();

        RemainingStockResponseDto remainingStockResponseDto = getResponse(organizationId, endDate);
        List<RemainingItemStockResponseDto> entities = remainingStockResponseDto.getValue();

        List<RemainingEntity> remainingEntities = new ArrayList<>();

        log.debug("Получено {} записей остатков из 1С", entities.size());

        AtomicLong totalRecordsLoaded = new AtomicLong(0);
        int savedCount = 0;
        long saveStartTime = System.currentTimeMillis();

        for (RemainingItemStockResponseDto entity : entities) {
            if (entity.getOrganizationKey().compareTo(organizationId) == 0) {
                remainingEntities.add(RemainingEntity.builder()
                                .date(endDate)
                                .organizationKey(entity.getOrganizationKey())
                                .nomenclatureKey(entity.getNomenclatureKey())
                                .characteristicKey(entity.getCharacteristicKey())
                                .batchKey(entity.getBatchKey())
                                .quantityBalance(entity.getQuantityBalance())
                                .amountBalance(entity.getAmountBalance())
                        .build());
                savedCount++;
            } else {
                log.debug("❌ {}<->{} Разные организации!!", entity.getOrganizationKey(),organizationId);
            }
        }

        remainingRepository.saveAll(remainingEntities);

        totalRecordsLoaded.addAndGet(savedCount);
        long saveTime = System.currentTimeMillis() - saveStartTime;
        log.info("💾 Сохранено {} записей (всего: {}, время сохранения: {} мс)",
                savedCount, totalRecordsLoaded.get(), saveTime);

        long totalTime = System.currentTimeMillis() - startTime;

        log.info("===== ЗАВЕРШЕНИЕ ЗАГРУЗКИ =====");
        log.info("✅ Всего загружено записей: {}", totalRecordsLoaded.get());
        log.info("⏱️ Общее время выполнения: {} мс ({} сек)", totalTime, totalTime / 1000);
        log.info("📊 Средняя скорость: {} записей/сек",
                totalRecordsLoaded.get() / (totalTime / 1000 > 0 ? totalTime / 1000 : 1));

        Page<RemainingEntity> result = remainingRepository.findAll(PageRequest.of(0, 10));
        log.info("📄 Возвращаем первые {} записей из {} всего", result.getNumberOfElements(), result.getTotalElements());

        return result;
    }

    private RemainingStockResponseDto getResponse(
            UUID guid,
            LocalDateTime endDate
    ) {
        log.debug("📡 Запрос к 1С: получение остатков для организации {}", guid);

        String url = String.format("/AccumulationRegister_Запасы/Balance(" +
                "Period=datetime'" + endDate + "'" +
                "Condition='cast(Организация_Key, 'Catalog_Организации') eq guid'%s'')" +
                "?" +
                "$select=Организация_Key,Номенклатура_Key, Характеристика_Key, Партия_Key, КоличествоBalance, СуммаBalance&" +
                "$format=json", guid);

        log.debug("URL запроса: {}", url.replaceAll("['\"]", ""));

        try {
            long requestStart = System.currentTimeMillis();
            RemainingStockResponseDto response = restClientConfig.restClient().get()
                    .uri(url)
                    .retrieve()
                    .body(RemainingStockResponseDto.class);

            log.debug("✅ Запрос выполнен за {} мс", System.currentTimeMillis() - requestStart);
            return response;

        } catch (Exception e) {
            log.error("❌ Ошибка при получении остатков из 1С: {}", e.getMessage(), e);
            throw new RuntimeException("Ошибка получения данных из 1С", e);
        }
    }
}
