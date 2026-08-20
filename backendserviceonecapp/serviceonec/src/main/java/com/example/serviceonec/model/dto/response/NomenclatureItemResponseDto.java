package com.example.serviceonec.model.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class NomenclatureItemResponseDto {
    @JsonProperty("Description")
    private String description;

    @JsonProperty("Code")
    private String code;

    @JsonProperty("ЕдиницаИзмерения_Key")
    private String measurementUnitKey;

    @JsonProperty("КатегорияНоменклатуры_Key")
    private String categoriesKey;

    @JsonProperty("Ref_Key")
    private String refKey;
}
