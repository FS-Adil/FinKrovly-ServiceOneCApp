package com.example.serviceonec.Monitoring.dto.output;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonitoringPmoPricesOfItemOutputResponseDto {

    @JsonProperty("odata.metadata")
    private String odataMetadata;

    @JsonProperty("value")
    private List<PriceRecord> value;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PriceRecord {

        @JsonProperty("Period")
        private LocalDateTime period;

        @JsonProperty("ВидЦен_Key")
        private UUID priceTypeKey;

        @JsonProperty("Номенклатура_Key")
        private UUID nomenclatureKey;

        @JsonProperty("Характеристика_Key")
        private UUID characteristicKey;

        @JsonProperty("Актуальность")
        private Boolean relevance;

        @JsonProperty("Цена")
        private BigDecimal price;

        @JsonProperty("ВключаяХарактеристики")
        private Boolean includingCharacteristics;
    }

}
