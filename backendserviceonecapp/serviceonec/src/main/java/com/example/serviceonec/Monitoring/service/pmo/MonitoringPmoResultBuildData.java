package com.example.serviceonec.Monitoring.service.pmo;

import com.example.serviceonec.Monitoring.dto.input.MonitoringPmoInputResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class MonitoringPmoResultBuildData {

    private final MonitoringPmoDetailsBuildData monitoringPmoDetailsBuildData;

    public MonitoringPmoInputResponseDto buildData() {
        List<MonitoringPmoInputResponseDto.OrderDetail> detailsOne = buildDetailsOne();
        List<MonitoringPmoInputResponseDto.OrderDetail> detailsTwo = buildDetailsTwo();

        MonitoringPmoInputResponseDto.OrderPmo orderPmoOne = buildOrderPmo(detailsOne);
        MonitoringPmoInputResponseDto.OrderPmo orderPmoTwo = buildOrderPmo(detailsTwo);

        return MonitoringPmoInputResponseDto.builder()
                .orderPmoOne(orderPmoOne)
                .orderPmoTwo(orderPmoTwo)
                .build();
    }

    private List<MonitoringPmoInputResponseDto.OrderDetail> buildDetailsTwo() {
        return monitoringPmoDetailsBuildData.getDetailsPmoTwo();
    }

    private List<MonitoringPmoInputResponseDto.OrderDetail> buildDetailsOne() {
        return Arrays.asList(
                MonitoringPmoInputResponseDto.OrderDetail.builder()
                        .author("Смирнов А.А.")
                        .orderNumber("ORD-2024101")
                        .date("2024-02-10")
                        .comment("Региональная доставка")
                        .status("paid")
                        .build(),
                MonitoringPmoInputResponseDto.OrderDetail.builder()
                        .author("Фёдоров Ф.Ф.")
                        .orderNumber("ORD-2024102")
                        .date("2024-02-11")
                        .comment("Экспресс")
                        .status("paid")
                        .build(),
                MonitoringPmoInputResponseDto.OrderDetail.builder()
                        .author("Григорьев Г.Г.")
                        .orderNumber("ORD-2024103")
                        .date("2024-02-12")
                        .comment("Ожидает оплаты")
                        .status("unpaid")
                        .build(),
                MonitoringPmoInputResponseDto.OrderDetail.builder()
                        .author("Алексеев А.А.")
                        .orderNumber("ORD-2024104")
                        .date("2024-02-13")
                        .comment("На складе")
                        .status("paid")
                        .build(),
                MonitoringPmoInputResponseDto.OrderDetail.builder()
                        .author("Дмитриев Д.Д.")
                        .orderNumber("ORD-2024105")
                        .date("2024-02-14")
                        .comment("Оплачен")
                        .status("paid")
                        .build()
        );
    }

    private MonitoringPmoInputResponseDto.OrderPmo buildOrderPmo(List<MonitoringPmoInputResponseDto.OrderDetail> details) {
        long paidCount = details.stream()
                .filter(d -> "paid".equals(d.getStatus()))
                .count();
        long unpaidCount = details.stream()
                .filter(d -> "unpaid".equals(d.getStatus()))
                .count();
        long processingCount = details.stream()
                .filter(d -> "processing".equals(d.getStatus()))
                .count();

        return MonitoringPmoInputResponseDto.OrderPmo.builder()
                .total(details.size())
                .paid((int) paidCount)
                .unpaid((int) unpaidCount)
                .processing((int) processingCount)
                .details(details)
                .build();
    }

}
