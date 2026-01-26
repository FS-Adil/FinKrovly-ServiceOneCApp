package com.example.serviceonec.service.invoice;

import com.example.serviceonec.config.RestClientConfig;
import com.example.serviceonec.model.dto.response.invoice.InvoiceStocksItemResponseDto;
import com.example.serviceonec.model.dto.response.invoice.InvoiceStocksResponseDto;
import com.example.serviceonec.model.entity.invoice.InvoiceStocksEntity;
import com.example.serviceonec.model.mapper.incoice.InvoiceStocksMapper;
import com.example.serviceonec.repository.invoice.InvoiceStocksRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class InvoiceStocksServiceImpl implements InvoiceStocksService {

    private final RestClientConfig restClientConfig;
    private final InvoiceStocksRepository invoiceStocksRepository;
    private final InvoiceStocksMapper invoiceStocksMapper;

    @Override
    public List<InvoiceStocksEntity> getAllInvoiceStocks() {

        invoiceStocksRepository.deleteAll();

//        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
//        String startStr = startDate.format(formatter);
//        String endStr = endDate.format(formatter);

        boolean isStop = true;
        int top = 100;
        int skip = 0;

        while (isStop) {

            log.info("------> Цикл с данными запроса: top({}) - skip({})", top, skip);

            InvoiceStocksResponseDto invoiceStocksResponseDto = getInvoiceStocks(top, skip);

            if (invoiceStocksResponseDto.getValue().isEmpty()) {
                isStop = false;
            }

            for (InvoiceStocksItemResponseDto value : invoiceStocksResponseDto.getValue()) {
                invoiceStocksRepository.save(invoiceStocksMapper.toEntity(value));
            }

            skip+=top;

            try {
                TimeUnit.MILLISECONDS.sleep(20);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        log.info("------> Все ЗАПАСЫ из приходников в 1с найдены и сохранены в базу");
        return invoiceStocksRepository.findAll();
    }

    private InvoiceStocksResponseDto getInvoiceStocks(Integer top, Integer skip) {
        log.info("------> Старт метода по поиску в 1с всех ЗАПАСОВ из приходника");

        String url = String.format("/Document_ПриходнаяНакладная_Запасы?" +
//                "$filter=Date ge datetime'" + startStr + "' " +
//                    "and Date le datetime'" + endStr + "'" +
//                "&" +
                "$select=Ref_Key,LineNumber,Номенклатура_Key,Характеристика_Key,Количество,ЕдиницаИзмерения,Цена&" +
                "$top=%s&$skip=%s&" +
                "$format=json", top, skip);

        InvoiceStocksResponseDto response;

        try {

            response = restClientConfig.restClient().get()
                    .uri(url)
                    .retrieve()
                    .body(InvoiceStocksResponseDto.class);

        } catch (Exception e) {
            // Логирование ошибки
            log.error(
                    String.format("Ошибка при получении ЗАПАСОВ из приходных накладных за период с %s по %s", 1, 2), String.valueOf(e)
            );
            throw new RuntimeException("Ошибка получения данных из 1С", e);
        }

        log.info("------> Конец метода по поиску в 1с всех ЗАПАСОВ из приходников");
        return response;
    }
}
