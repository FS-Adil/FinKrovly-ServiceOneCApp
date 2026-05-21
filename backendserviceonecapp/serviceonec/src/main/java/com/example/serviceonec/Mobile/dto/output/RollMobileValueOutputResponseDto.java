package com.example.serviceonec.Mobile.dto.output;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Setter
@Getter
public class RollMobileValueOutputResponseDto {

    @JsonProperty("Организация_Key")
    private UUID organizationKey;

    @JsonProperty("СтруктурнаяЕдиница")
    private UUID structuralUnit;

    @JsonProperty("Номенклатура_Key")
    private UUID nomenclatureKey;

    @JsonProperty("Характеристика_Key")
    private UUID characteristicKey;

    @JsonProperty("Партия_Key")
    private UUID batchKey;

    @JsonProperty("КоличествоBalance")
    private BigDecimal quantityBalance;

}
