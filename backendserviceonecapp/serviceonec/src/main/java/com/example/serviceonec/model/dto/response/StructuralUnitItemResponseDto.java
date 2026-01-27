package com.example.serviceonec.model.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.UUID;

@Data
public class StructuralUnitItemResponseDto {

    @JsonProperty("Ref_Key")
    private UUID refKey;

    @JsonProperty("Code")
    private String code;

    @JsonProperty("Description")
    private String description;

    @JsonProperty("ТипСтруктурнойЕдиницы")
    private String structuralUnitType;

    @JsonProperty("Организация_Key")
    private UUID organizationKey;

}
