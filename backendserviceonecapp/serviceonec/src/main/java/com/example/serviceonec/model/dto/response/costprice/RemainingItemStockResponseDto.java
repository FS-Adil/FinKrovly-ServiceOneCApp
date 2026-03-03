package com.example.serviceonec.model.dto.response.costprice;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class RemainingItemStockResponseDto {

    @JsonProperty("Организация_Key")
    private UUID organizationKey;

    @JsonProperty("Номенклатура_Key")
    private UUID nomenclatureKey;

    @JsonProperty("Характеристика_Key")
    private UUID characteristicKey;

    @JsonProperty("Партия_Key")
    private UUID batchKey;

    @JsonProperty("КоличествоBalance")
    private BigDecimal quantityBalance;

    @JsonProperty("СуммаBalance")
    private BigDecimal amountBalance;
}
