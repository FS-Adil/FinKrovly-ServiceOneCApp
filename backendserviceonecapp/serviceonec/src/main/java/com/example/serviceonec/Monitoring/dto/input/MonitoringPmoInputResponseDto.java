package com.example.serviceonec.Monitoring.dto.input;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MonitoringPmoInputResponseDto {

    @JsonProperty("orderPmoOne")
    private OrderPmo orderPmoOne;

    @JsonProperty("orderPmoTwo")
    private OrderPmo orderPmoTwo;

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OrderPmo {
        private int total;
        private int paid;
        private int unpaid;
        private int processing;
        private List<OrderDetail> details;
    }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OrderDetail {
        private String author;
        private String orderNumber;
        private String date;
        private String comment;
        private String status;
    }

}
