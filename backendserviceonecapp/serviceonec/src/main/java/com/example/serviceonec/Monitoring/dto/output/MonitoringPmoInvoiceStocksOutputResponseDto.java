package com.example.serviceonec.Monitoring.dto.output;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonitoringPmoInvoiceStocksOutputResponseDto {

    @JsonProperty("odata.metadata")
    private String odataMetadata;

    @JsonProperty("Ref_Key")
    private UUID refKey;

    @JsonProperty("LineNumber")
    private String lineNumber;

    @JsonProperty("Номенклатура_Key")
    private UUID nomenclatureKey;

    @JsonProperty("Характеристика_Key")
    private UUID characteristicKey;

    @JsonProperty("Цена")
    private BigDecimal price;

    @JsonProperty("Количество")
    private BigDecimal quantity;

}
