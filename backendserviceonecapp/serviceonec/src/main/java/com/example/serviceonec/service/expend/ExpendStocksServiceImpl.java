package com.example.serviceonec.service.expend;

import com.example.serviceonec.config.RestClientConfig;
import com.example.serviceonec.model.dto.response.expend.ExpendStocksItemResponseDto;
import com.example.serviceonec.model.dto.response.expend.ExpendStocksResponseDto;
import com.example.serviceonec.model.dto.response.invoice.InvoiceStocksItemResponseDto;
import com.example.serviceonec.model.dto.response.invoice.InvoiceStocksResponseDto;
import com.example.serviceonec.model.entity.expend.ExpendStocksEntity;
import com.example.serviceonec.model.mapper.expend.ExpendStocksMapper;
import com.example.serviceonec.repository.expend.ExpendRepository;
import com.example.serviceonec.repository.expend.ExpendStocksRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class ExpendStocksServiceImpl implements ExpendStocksService {

    private final RestClientConfig restClientConfig;
    private final ExpendStocksRepository expendStocksRepository;
    private final ExpendStocksMapper expendStocksMapper;

    @Override
    public Page<ExpendStocksEntity> getAllExpendStocks() {

        expendStocksRepository.deleteAll();

//        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
//        String startStr = startDate.format(formatter);
//        String endStr = endDate.format(formatter);

        boolean isStop = true;
        int top = 100;
        int skip = 0;

        while (isStop) {

            log.info("------> Цикл с данными запроса: top({}) - skip({})", top, skip);

            ExpendStocksResponseDto expendStocksResponseDto = getExpendStocks(top, skip);

            if (expendStocksResponseDto.getValue().isEmpty()) {
                isStop = false;
            }

            for (ExpendStocksItemResponseDto value : expendStocksResponseDto.getValue()) {
                expendStocksRepository.save(expendStocksMapper.toEntity(value));
            }

            skip+=top;

            try {
                TimeUnit.MILLISECONDS.sleep(20);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        log.info("------> Все ЗАПАСЫ из расходников в 1с найдены и сохранены в базу");

        return expendStocksRepository.findAll(PageRequest.of(0, 10));
    }

    private ExpendStocksResponseDto getExpendStocks(Integer top, Integer skip) {
        log.info("------> Старт метода по поиску в 1с всех ЗАПАСОВ из расходника");

        String url = String.format("/Document_РасходнаяНакладная_Запасы?" +
//                "$filter=Date ge datetime'" + startStr + "' " +
//                    "and Date le datetime'" + endStr + "'" +
//                "&" +
                "$select=Ref_Key,LineNumber,Номенклатура_Key,Характеристика_Key,Количество,ЕдиницаИзмерения,Цена&" +
                "$top=%s&$skip=%s&" +
                "$format=json", top, skip);

        ExpendStocksResponseDto response;

        try {

            response = restClientConfig.restClient().get()
                    .uri(url)
                    .retrieve()
                    .body(ExpendStocksResponseDto.class);

        } catch (Exception e) {
            // Логирование ошибки
            log.error(
                    String.format("Ошибка при получении ЗАПАСОВ из расходных накладных за период с %s по %s", 1, 2), String.valueOf(e)
            );
            throw new RuntimeException("Ошибка получения данных из 1С", e);
        }

        log.info("------> Конец метода по поиску в 1с всех ЗАПАСОВ из расходников");
        return response;
    }
}
