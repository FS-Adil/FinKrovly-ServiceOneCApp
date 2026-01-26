package com.example.serviceonec.model.dto.response.expend;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class ExpendItemResponseDto {

    @JsonProperty("Ref_Key")
    private UUID refKey;

    @JsonProperty("Number")
    private String number;

    @JsonProperty("Организация_Key")
    private UUID organizationKey;

    @JsonProperty("Заказ")
    private UUID docOrder;

    @JsonProperty("СтруктурнаяЕдиница_Key")
    private UUID structuralUnitKey;

    //    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonProperty("Date")
    private LocalDateTime date;

    @JsonProperty("ВидОперации")
    private String operationType;

}
