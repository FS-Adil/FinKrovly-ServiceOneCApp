package com.example.serviceonec.service.expend;

import com.example.serviceonec.config.RestClientConfig;
import com.example.serviceonec.model.dto.response.expend.ExpendItemResponseDto;
import com.example.serviceonec.model.dto.response.expend.ExpendResponseDto;
import com.example.serviceonec.model.entity.expend.ExpendEntity;
import com.example.serviceonec.model.mapper.expend.ExpendMapper;
import com.example.serviceonec.repository.expend.ExpendRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class ExpendServiceImpl implements ExpendService {

    private final RestClientConfig restClientConfig;
    private final ExpendRepository expendRepository;
    private final ExpendMapper expendMapper;

    @Override
    public Page<ExpendEntity> getAllExpend(
            UUID organizationId,
            LocalDateTime startDate,
            LocalDateTime endDate
    ) {

        log.info("-----> ExpendServiceImpl -----> getAllExpend");

        expendRepository.deleteAll();

        boolean isStop = true;
        int top = 500;
        int skip = 0;

        while (isStop) {

//            log.info("------> Цикл с данными запроса: top({}) - skip({})", top, skip);

            ExpendResponseDto expendResponseDto = getExpend(
                    organizationId,
                    startDate,
                    endDate,
                    top,
                    skip
            );

            if (expendResponseDto.getValue().isEmpty()) {
                isStop = false;
            }

            try {
                for (ExpendItemResponseDto value : expendResponseDto.getValue()) {
                    expendRepository.save(expendMapper.toEntity(value));
                }
            } catch (DataIntegrityViolationException e) {
                log.error("Ошибка целостности данных: {}", e.getMessage());
            }

            skip+=top;

            try {
                TimeUnit.MILLISECONDS.sleep(20);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        log.info("------> Все расходники из 1с за период с {} по {} найдены и сохранены в базу", startDate, endDate);

        return expendRepository.findAll(PageRequest.of(0, 10));
    }

    private ExpendResponseDto getExpend(
            UUID organizationId,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Integer top,
            Integer skip
    ) {
//        log.info("------> Старт метода по поиску в 1с всех расходников");
        String url = String.format("/Document_РасходнаяНакладная?" +
                "$filter=Posted eq true" +
                " and Организация_Key eq guid'" + organizationId + "'" +
                    " and Date ge datetime'" + startDate + "'" +
                    " and Date le datetime'" + endDate + "'" +
                "&" +
                "$select=Number,Date,Организация_Key,ВидОперации,Ref_Key,СтруктурнаяЕдиница_Key,Заказ&" +
                "$top=%s&$skip=%s&" +
                "$orderby=Date desc&" +
                "$format=json", top, skip);

        ExpendResponseDto response;

        try {

            response = restClientConfig.restClient().get()
                    .uri(url)
                    .retrieve()
                    .body(ExpendResponseDto.class);

        } catch (Exception e) {
            // Логирование ошибки
            log.error(
                    String.format("Ошибка при получении Расходных накладных за период с %s по %s", startDate, endDate), String.valueOf(e)
            );
            throw new RuntimeException("Ошибка получения данных из 1С", e);
        }

//        log.info("------> Конец метода по поиску в 1с всех расходников");
        return response;
    }
}
