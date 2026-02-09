package com.example.serviceonec.model.dto.response.expend;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ExpendStocksItemResponseDto {

    @JsonProperty("Ref_Key")
    private String refKey;

    @JsonProperty("LineNumber")
    private String lineNumber;

    @JsonProperty("Номенклатура_Key")
    private String nomenclatureKey;

    @JsonProperty("Характеристика_Key")
    private String characteristicKey;

    @JsonProperty("Количество")
    private BigDecimal quantity;

    @JsonProperty("ЕдиницаИзмерения")
    private String measurementUnit;

    @JsonProperty("Цена")
    private BigDecimal price;

}
