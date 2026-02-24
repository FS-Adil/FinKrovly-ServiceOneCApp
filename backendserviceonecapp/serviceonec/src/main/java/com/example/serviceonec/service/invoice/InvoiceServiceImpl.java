package com.example.serviceonec.service.invoice;

import com.example.serviceonec.config.RestClientConfig;
import com.example.serviceonec.model.dto.response.invoice.InvoiceResponseDto;
import com.example.serviceonec.model.dto.response.invoice.InvoiceItemResponseDto;
import com.example.serviceonec.model.entity.invoice.InvoiceEntity;
import com.example.serviceonec.model.mapper.invoice.InvoiceMapper;
import com.example.serviceonec.repository.invoice.InvoiceRepository;
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
public class InvoiceServiceImpl implements InvoiceService {

    private final RestClientConfig restClientConfig;
    private final InvoiceMapper invoiceMapper;
    private final InvoiceRepository invoiceRepository;

    @Override
    public Page<InvoiceEntity> getAllInvoice(
            UUID organizationId,
            LocalDateTime endDate
    ) {
        log.info("-------> InvoiceServiceImpl -------> getAllInvoice");

        invoiceRepository.deleteAll();

        boolean isStop = true;
        int top = 500;
        int skip = 0;

        while (isStop) {

//            log.info("------> Цикл с данными запроса: top({}) - skip({})", top, skip);

            InvoiceResponseDto invoiceResponseDto = getInvoice(
                    organizationId,
                    endDate,
                    top,
                    skip
            );

            if (invoiceResponseDto.getValue().isEmpty()) {
                isStop = false;
            }

            try {
                for (InvoiceItemResponseDto value : invoiceResponseDto.getValue()) {
                    invoiceRepository.save(invoiceMapper.toEntity(value));
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

        log.info("------> Все приходники из 1с найдены и сохранены в базу");
        return invoiceRepository.findAll(PageRequest.of(0, 10));
    }


    private InvoiceResponseDto getInvoice(
            UUID organizationId,
            LocalDateTime endDate,
            Integer top,
            Integer skip
    ) {
//        log.info("------> Старт метода по поиску в 1с всех приходников");
        String url = String.format("/Document_ПриходнаяНакладная?" +
                "$filter=Posted eq true" +
                " and Организация_Key eq guid'" + organizationId + "'" +
//                    "and Date ge datetime'" + startStr + "' " +
                    "and Date le datetime'" + endDate + "'" +
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

//        log.info("------> Конец метода по поиску в 1с всех приходников");
        return response;
    }
}
