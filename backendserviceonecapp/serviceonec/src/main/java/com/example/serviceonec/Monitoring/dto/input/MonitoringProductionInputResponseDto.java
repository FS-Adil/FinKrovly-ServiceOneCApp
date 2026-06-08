package com.example.serviceonec.Monitoring.dto.input;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MonitoringProductionInputResponseDto {

    @JsonProperty("orderProductionOne")
    private OrderProductionOne orderProductionOne;

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OrderProductionOne {
        private int total;
        private int paid;
        private int unpaid;
        private int processing;
        private List<ProductionDetail> details;
    }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ProductionDetail {
        private String author;
        private String orderNumber;
        private String date;
        private String comment;
        private String status;
    }

}
