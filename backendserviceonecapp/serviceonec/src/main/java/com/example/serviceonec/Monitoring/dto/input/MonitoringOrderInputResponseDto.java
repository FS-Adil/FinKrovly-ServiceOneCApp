package com.example.serviceonec.Monitoring.dto.input;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MonitoringOrderInputResponseDto {

        @JsonProperty("orderExecutabilityOne")
        private OrderExecutability orderExecutabilityOne;

        @JsonProperty("orderExecutabilityTwo")
        private OrderExecutability orderExecutabilityTwo;

        @JsonProperty("orderExecutabilityThree")
        private OrderExecutability orderExecutabilityThree;

        @Getter
        @Setter
        @Builder
        @AllArgsConstructor
        @NoArgsConstructor
        public static class OrderExecutability {
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
