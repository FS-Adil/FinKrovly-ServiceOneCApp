package com.example.serviceonec.Monitoring.service.order;

import com.example.serviceonec.Monitoring.dto.input.MonitoringOrderInputResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class MonitoringOrderResultBuildData {

    public MonitoringOrderInputResponseDto buildTestData() {
        List<MonitoringOrderInputResponseDto.OrderDetail> testOneDetails = buildTestDetails();
        List<MonitoringOrderInputResponseDto.OrderDetail> testTwoDetails = buildTestDetails();
        List<MonitoringOrderInputResponseDto.OrderDetail> testThreeDetails = buildTestDetails();

        MonitoringOrderInputResponseDto.OrderExecutability resultOne = buildTestResult(testOneDetails);
        MonitoringOrderInputResponseDto.OrderExecutability resultTwo = buildTestResult(testTwoDetails);
        MonitoringOrderInputResponseDto.OrderExecutability resultThree = buildTestResult(testThreeDetails);

        return MonitoringOrderInputResponseDto.builder()
                .orderExecutabilityOne(resultOne)
                .orderExecutabilityTwo(resultTwo)
                .orderExecutabilityThree(resultThree)
                .build();
    }

    private List<MonitoringOrderInputResponseDto.OrderDetail> buildTestDetails() {
        return Arrays.asList(
                MonitoringOrderInputResponseDto.OrderDetail.builder()
                        .author("Смирнов А.А.")
                        .orderNumber("ORD-2024101")
                        .date("2024-02-10")
                        .comment("Региональная доставка")
                        .status("paid")
                        .build(),
                MonitoringOrderInputResponseDto.OrderDetail.builder()
                        .author("Фёдоров Ф.Ф.")
                        .orderNumber("ORD-2024102")
                        .date("2024-02-11")
                        .comment("Экспресс")
                        .status("paid")
                        .build(),
                MonitoringOrderInputResponseDto.OrderDetail.builder()
                        .author("Григорьев Г.Г.")
                        .orderNumber("ORD-2024103")
                        .date("2024-02-12")
                        .comment("Ожидает оплаты")
                        .status("unpaid")
                        .build(),
                MonitoringOrderInputResponseDto.OrderDetail.builder()
                        .author("Алексеев А.А.")
                        .orderNumber("ORD-2024104")
                        .date("2024-02-13")
                        .comment("На складе")
                        .status("paid")
                        .build(),
                MonitoringOrderInputResponseDto.OrderDetail.builder()
                        .author("Дмитриев Д.Д.")
                        .orderNumber("ORD-2024105")
                        .date("2024-02-14")
                        .comment("Оплачен")
                        .status("paid")
                        .build()
        );
    }

    private MonitoringOrderInputResponseDto.OrderExecutability buildTestResult(List<MonitoringOrderInputResponseDto.OrderDetail> details) {
        long paidCount = details.stream()
                .filter(d -> "paid".equals(d.getStatus()))
                .count();
        long unpaidCount = details.stream()
                .filter(d -> "unpaid".equals(d.getStatus()))
                .count();
        long processingCount = details.stream()
                .filter(d -> "processing".equals(d.getStatus()))
                .count();

        return MonitoringOrderInputResponseDto.OrderExecutability.builder()
                .total(details.size())
                .paid((int) paidCount)
                .unpaid((int) unpaidCount)
                .processing((int) processingCount)
                .details(details)
                .build();
    }


}
