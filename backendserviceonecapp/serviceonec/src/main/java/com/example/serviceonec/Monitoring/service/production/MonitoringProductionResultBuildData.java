package com.example.serviceonec.Monitoring.service.production;

import com.example.serviceonec.Monitoring.dto.input.MonitoringProductionInputResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class MonitoringProductionResultBuildData {

    private final MonitoringProductionDetailsBuildData monitoringProductionDetailsBuildData;

    public MonitoringProductionInputResponseDto buildData() {
        List<MonitoringProductionInputResponseDto.ProductionDetail> detailsOne = buildDetailsOne();

        MonitoringProductionInputResponseDto.OrderProduction productionOne = buildProduction(detailsOne);

        return MonitoringProductionInputResponseDto.builder()
                .orderProductionOne(productionOne)
                .build();
    }

    private List<MonitoringProductionInputResponseDto.ProductionDetail> buildDetailsOne() {
        return monitoringProductionDetailsBuildData.getDetailsProductionOne();
    }

    private MonitoringProductionInputResponseDto.OrderProduction buildProduction(List<MonitoringProductionInputResponseDto.ProductionDetail> details) {
        long paidCount = details.stream()
                .filter(d -> "paid".equals(d.getStatus()))
                .count();
        long unpaidCount = details.stream()
                .filter(d -> "unpaid".equals(d.getStatus()))
                .count();
        long processingCount = details.stream()
                .filter(d -> "processing".equals(d.getStatus()))
                .count();

        return MonitoringProductionInputResponseDto.OrderProduction.builder()
                .total(details.size())
                .paid((int) paidCount)
                .unpaid((int) unpaidCount)
                .processing((int) processingCount)
                .details(details)
                .build();
    }

}
