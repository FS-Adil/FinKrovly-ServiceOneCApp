package com.example.serviceonec.model.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class StructuralUnitResponseDto {

    @JsonProperty("odata.metadata")
    private String metadata;

    @JsonProperty("value")
    private List<StructuralUnitItemResponseDto> value;

}
