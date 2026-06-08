package com.example.serviceonec.Monitoring.service;

import com.example.serviceonec.Monitoring.dto.input.MonitoringOrderInputResponseDto;
import com.example.serviceonec.Monitoring.dto.input.MonitoringProductionInputResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MonitoringProductionServiceImpl implements MonitoringProductionService {

    @Override
    public Optional<MonitoringProductionInputResponseDto> getProductions() {
        return Optional.of(buildTestData());
    }

    private MonitoringProductionInputResponseDto buildTestData() {
        List<MonitoringProductionInputResponseDto.ProductionDetail> testDetails = buildTestDetails();

        MonitoringProductionInputResponseDto.OrderProductionOne orderProductionOne = buildOrderProduction(testDetails);

        return MonitoringProductionInputResponseDto.builder()
                .orderProductionOne(orderProductionOne)
                .build();
    }

    private List<MonitoringProductionInputResponseDto.ProductionDetail> buildTestDetails() {
        return Arrays.asList(
                MonitoringProductionInputResponseDto.ProductionDetail.builder()
                        .author("Смирнов А.А.")
                        .orderNumber("ORD-2024101")
                        .date("2024-02-10")
                        .comment("Региональная доставка")
                        .status("paid")
                        .build(),
                MonitoringProductionInputResponseDto.ProductionDetail.builder()
                        .author("Фёдоров Ф.Ф.")
                        .orderNumber("ORD-2024102")
                        .date("2024-02-11")
                        .comment("Экспресс")
                        .status("paid")
                        .build(),
                MonitoringProductionInputResponseDto.ProductionDetail.builder()
                        .author("Григорьев Г.Г.")
                        .orderNumber("ORD-2024103")
                        .date("2024-02-12")
                        .comment("Ожидает оплаты")
                        .status("paid")
                        .build(),
                MonitoringProductionInputResponseDto.ProductionDetail.builder()
                        .author("Алексеев А.А.")
                        .orderNumber("ORD-2024104")
                        .date("2024-02-13")
                        .comment("На складе")
                        .status("paid")
                        .build(),
                MonitoringProductionInputResponseDto.ProductionDetail.builder()
                        .author("Дмитриев Д.Д.")
                        .orderNumber("ORD-2024105")
                        .date("2024-02-14")
                        .comment("Оплачен")
                        .status("paid")
                        .build()
        );
    }

    private MonitoringProductionInputResponseDto.OrderProductionOne buildOrderProduction(List<MonitoringProductionInputResponseDto.ProductionDetail> details) {
        long paidCount = details.stream()
                .filter(d -> "paid".equals(d.getStatus()))
                .count();
        long unpaidCount = details.stream()
                .filter(d -> "unpaid".equals(d.getStatus()))
                .count();
        long processingCount = details.stream()
                .filter(d -> "processing".equals(d.getStatus()))
                .count();

        return MonitoringProductionInputResponseDto.OrderProductionOne.builder()
                .total(details.size())
                .paid((int) paidCount)
                .unpaid((int) unpaidCount)
                .processing((int) processingCount)
                .details(details)
                .build();
    }
}
