package com.example.serviceonec.service;

import com.example.serviceonec.config.RestClientConfig;
import com.example.serviceonec.model.dto.response.InvoiceResponseDto;
import com.example.serviceonec.model.dto.response.InvoiceValueResponseDto;
import com.example.serviceonec.model.entity.InvoiceEntity;
import com.example.serviceonec.model.mapper.InvoiceMapper;
import com.example.serviceonec.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class InvoiceServiceImpl implements InvoiceService{

    private final RestClientConfig restClientConfig;
    private final InvoiceMapper invoiceMapper;
    private final InvoiceRepository invoiceRepository;

    @Override
    public List<InvoiceEntity> getAllInvoice(
//            LocalDateTime startDate, LocalDateTime endDate
    ) {

        invoiceRepository.deleteAll();

//        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
//        String startStr = startDate.format(formatter);
//        String endStr = endDate.format(formatter);

        boolean isStop = true;
        int top = 100;
        int skip = 0;

        while (isStop) {

            log.info("------> Цикл с данными запроса: top({}) - skip({})", top, skip);

            InvoiceResponseDto invoiceResponseDto = getInvoice(top, skip);

            if (invoiceResponseDto.getValue().isEmpty()) {
                isStop = false;
            }

            for (InvoiceValueResponseDto value : invoiceResponseDto.getValue()) {
                invoiceRepository.save(invoiceMapper.toEntity(value));
            }

            skip+=top;

            try {
                TimeUnit.MILLISECONDS.sleep(20);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        log.info("------> Все приходники из 1с найдены и сохранены в базу");
        return invoiceRepository.findAll();
    }


    private InvoiceResponseDto getInvoice(Integer top, Integer skip) {
        log.info("------> Старт метода по поиску в 1с всех приходников");
        String url = String.format("/Document_ПриходнаяНакладная?" +
                "$filter=Posted eq true" +
//                " and ВидОперации eq 'ПоступлениеОтПоставщика'" +
//                    "and Date ge datetime'" + startStr + "' " +
//                    "and Date le datetime'" + endStr + "'" +
                "&" +
                "$select= Number,Date,Ref_Key,Организация_Key,ВидОперации&" +
                "$top=%s&$skip=%s&" +
                "$orderby=Date desc&" +
                "$format=json", top, skip);

        InvoiceResponseDto response;

        try {

            response = restClientConfig.restClient().get()
                    .uri(url)
                    .retrieve()
                    .body(InvoiceResponseDto.class);

        } catch (Exception e) {
            // Логирование ошибки
            log.error(
                    String.format("Ошибка при получении Приходных накладных за период с %s по %s", 1, 2), String.valueOf(e)
            );
            throw new RuntimeException("Ошибка получения данных из 1С", e);
        }

        log.info("------> Конец метода по поиску в 1с всех приходников");
        return response;
    }
}
