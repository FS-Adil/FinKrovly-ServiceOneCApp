package com.example.serviceonec.Monitoring.service.pmo;

import com.example.serviceonec.Mobile.dto.output.RollMobileOutputResponseDto;
import com.example.serviceonec.Monitoring.dto.input.MonitoringPmoInputResponseDto;
import com.example.serviceonec.Monitoring.dto.output.MonitoringPmoInvoiceOutputResponseDto;
import com.example.serviceonec.Monitoring.dto.output.MonitoringPmoInvoiceStocksOutputResponseDto;
import com.example.serviceonec.Monitoring.dto.output.MonitoringPmoPricesOfItemOutputResponseDto;
import com.example.serviceonec.config.OneCProperties;
import com.example.serviceonec.config.RestClientConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class MonitoringPmoDetailsBuildData {

    private final RestClientConfig restClientConfig;
    private final OneCProperties oneCProperties;

    private final List<MonitoringPmoInputResponseDto.OrderDetail> resultListTwo = new ArrayList<>();

    public List<MonitoringPmoInputResponseDto.OrderDetail> getDetailsPmoTwo() {

        long methodStartTime = System.currentTimeMillis();

        resultListTwo.clear();

        Map<String, String> organizationMap = new HashMap<>();
        Map<String, String> counterpartyMap = new HashMap<>();

        organizationMap.put("ФинКровля", "32c959f5-bf9a-11e5-ab21-f46d0466b92f");
        organizationMap.put("НК Махачкала", "ed447d5f-10e4-11e9-80f4-000c29aa9162");
        organizationMap.put("НК Дербент", "e79c6ee7-3be9-11ec-815c-000c29f4122d");
        organizationMap.put("КК Пятигорск", "b125e94a-af5f-11ee-8676-a6875c54f300");

        counterpartyMap.put("Цех Надежна Крыша Поставщик", "3501d0c0-171e-11e9-80f4-000c29aa9162");
        counterpartyMap.put("Цех Финкровля Поставщик", "219d4b2f-171d-11e9-80f4-000c29aa9162");
        counterpartyMap.put("Цех НК Дербент Поставщик", "b7d5cab1-5cae-11ec-815c-000c29f4122d");
        counterpartyMap.put("Поставщик в Пятигорске", "4a766a6a-5364-11f0-aecf-00155d044f02");

        List<String[]> listOfArrays = new ArrayList<>();
        listOfArrays.add(new String[]{organizationMap.get("ФинКровля"), counterpartyMap.get("Цех Надежна Крыша Поставщик")});
        listOfArrays.add(new String[]{organizationMap.get("ФинКровля"), counterpartyMap.get("Цех НК Дербент Поставщик")});
        listOfArrays.add(new String[]{organizationMap.get("НК Махачкала"), counterpartyMap.get("Цех Финкровля Поставщик")});
        listOfArrays.add(new String[]{organizationMap.get("НК Махачкала"), counterpartyMap.get("Цех НК Дербент Поставщик")});
        listOfArrays.add(new String[]{organizationMap.get("НК Дербент"), counterpartyMap.get("Цех Надежна Крыша Поставщик")});
        listOfArrays.add(new String[]{organizationMap.get("НК Дербент"), counterpartyMap.get("Цех Финкровля Поставщик")});
        listOfArrays.add(new String[]{organizationMap.get("КК Пятигорск"), counterpartyMap.get("Поставщик в Пятигорске")});

        for (String[] array : listOfArrays) {

            // Получаем из 1с Приходники за предыдущий месяц, фильтруем по организациям и контрагентам
            MonitoringPmoInvoiceOutputResponseDto detailResponse = getResponseInvoice(array[0], array[1]);

            if (detailResponse.getValue().isEmpty()) {
                log.info("------> По данной комбинации --> {} --> {} --> нет приходников", array[0], array[1]);
                continue;
            }

            for (MonitoringPmoInvoiceOutputResponseDto.ReceiptInvoice receiptInvoice : detailResponse.getValue()) {

                // Получаем из 1с Запасы на приходники, фильтруем по ref_key
                MonitoringPmoInvoiceStocksOutputResponseDto invoiceStocks = getResponseInvoiceStocks(receiptInvoice.getRefKey().toString());

                if (invoiceStocks == null) {
                    continue;
                }
                log.debug("{}", invoiceStocks.getPrice());

                // Получаем из 1с все Цены Номенклатуры, фильтруем по виду цен "Цена между фирмами" и номенклатуре, и характеристике
                MonitoringPmoPricesOfItemOutputResponseDto pricesOfItems = getResponsePricesOfItems(
                        invoiceStocks.getNomenclatureKey(),
                        invoiceStocks.getCharacteristicKey()
                );

                if (pricesOfItems == null) {
                    continue;
                }

                if (pricesOfItems.getValue().isEmpty()) {
                    log.debug("Список цен на Товары пуст");
                    continue;
                }

                MonitoringPmoPricesOfItemOutputResponseDto.PriceRecord priceRecord = pricesOfItems.getValue().stream()
                        .max(Comparator.comparing(MonitoringPmoPricesOfItemOutputResponseDto.PriceRecord::getPeriod))
                        .orElse(null);

                BigDecimal priceOf = priceRecord.getPrice();

                if (invoiceStocks.getPrice().compareTo(priceOf) == 0) {
                    log.debug("Цена соответствует!");
                    continue;
                }

                addResultListTwo(receiptInvoice);
            }
        }

        long totalTime = System.currentTimeMillis() - methodStartTime;
        log.info("⏱️ Общее время выполнения: {} мс ({} сек)", totalTime, totalTime / 1000);

        return getResultListTwo();
    }

    private List<MonitoringPmoInputResponseDto.OrderDetail> getResultListTwo() {
        return resultListTwo;
    }

    private void addResultListTwo(MonitoringPmoInvoiceOutputResponseDto.ReceiptInvoice receiptInvoice) {
        resultListTwo.add(
                MonitoringPmoInputResponseDto.OrderDetail.builder()
                        .author(receiptInvoice.getAuthorKey().toString())
                        .orderNumber(receiptInvoice.getNumber())
                        .date(receiptInvoice.getDate().toString())
                        .comment(receiptInvoice.getComment())
                        .status("paid")
                        .build()
        );
    }

    private MonitoringPmoInvoiceOutputResponseDto getResponseInvoice(String organizationId, String counterpartyId) {

        log.info("------> Старт метода по поиску в 1с всех Приходников");

        LocalDateTime date = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);

        String url = String.format("/Document_ПриходнаяНакладная?" +
                "$filter=Posted eq true" +
                " and ВидОперации eq 'ПоступлениеОтПоставщика'" +
                " and Организация_Key eq guid'" + organizationId + "'" +
                " and Контрагент_Key eq guid'" + counterpartyId + "'" +
                " and Date ge datetime'" + date.minusMonths(1) + "'" +
                " and Date le datetime'" + date + "'" +
                "&" +
                "$select=Ref_Key, Number, Date, Автор_Key, Комментарий, Контрагент_Key, Организация_Key&" +
                "$orderby=Date desc&" +
                "$format=json");

//        log.info("url-->{}", url);

        MonitoringPmoInvoiceOutputResponseDto response;

        try {

            response = restClientConfig.restClient().get()
                    .uri(url)
                    .retrieve()
                    .body(MonitoringPmoInvoiceOutputResponseDto.class);

        } catch (Exception e) {
            // Логирование ошибки
            log.error(
                    String.format("Ошибка при получении Приходников"), String.valueOf(e)
            );
            throw new RuntimeException("Ошибка получения данных из 1С", e);
        }
        log.info("------> Конец метода по поиску в 1с всех Приходников");

        return response;
    }

    private MonitoringPmoInvoiceStocksOutputResponseDto getResponseInvoiceStocks(String refKey) {
        log.debug("------> Старт метода по поиску в 1с Запасы Приходника");

        String url = String.format("/Document_ПриходнаяНакладная_Запасы(" +
                "Ref_Key=guid'%s', LineNumber=1" +
                ")?" +
                "$select=Ref_Key, LineNumber, Номенклатура_Key, Характеристика_Key, Цена, Количество&" +
                "$format=json", refKey);

        try {

            MonitoringPmoInvoiceStocksOutputResponseDto response = restClientConfig.restClient().get()
                    .uri(url)
                    .retrieve()
                    .body(MonitoringPmoInvoiceStocksOutputResponseDto.class);

            log.debug("------> Конец метода по поиску в 1с Запасы Приходника");
            return response;
        } catch (Exception e) {
            // Логирование ошибки
            log.error(
                    String.format("Ошибка при получении Запасы Приходника"), String.valueOf(e)
            );
            return null;
        }
    }

    private MonitoringPmoPricesOfItemOutputResponseDto getResponsePricesOfItems(UUID nomenclatureKey, UUID characteristicKey) {

        log.debug("------> Старт метода по поиску в 1с Цены Номенклатуры");

        String url = String.format("/InformationRegister_ЦеныНоменклатуры/" +
                "?" +
                "$filter=ВидЦен_Key eq guid'%s' " +
                "and Номенклатура_Key eq guid'%s' " +
                "and Характеристика_Key eq guid'%s'&" +
                "$select=Period, ВидЦен_Key, Номенклатура_Key, Характеристика_Key, Актуальность, Цена, ВключаяХарактеристики&" +
                "$format=json",
                oneCProperties.getTypeOfPrices(),
                nomenclatureKey,
                characteristicKey
                );

        try {

            MonitoringPmoPricesOfItemOutputResponseDto response = restClientConfig.restClient().get()
                    .uri(url)
                    .retrieve()
                    .body(MonitoringPmoPricesOfItemOutputResponseDto.class);

            log.debug("------> Конец метода по поиску в 1с всех Цены Номенклатуры");
            return response;

        } catch (Exception e) {
            // Логирование ошибки
            log.error(
                    String.format("Ошибка при получении Списка Цены Номенклатуры"), String.valueOf(e)
            );
            return null;
        }
    }
}
