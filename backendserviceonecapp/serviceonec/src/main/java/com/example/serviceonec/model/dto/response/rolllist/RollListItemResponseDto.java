package com.example.serviceonec.model.dto.response.rolllist;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.UUID;

@Data
public class RollListItemResponseDto {

    @JsonProperty("Номенклатура_Key")
    private UUID nomenclatureKey;

    @JsonProperty("Характеристика_Key")
    private UUID characteristicKey;

    @JsonProperty("Партия_Key")
    private UUID batchKey;

    @JsonProperty("КоличествоBalance")
    private Double quantityBalance;

    @JsonProperty("СуммаBalance")
    private Double amountBalance;

}
