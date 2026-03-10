package com.example.serviceonec.model.dto.response.specification;

import com.example.serviceonec.model.dto.response.NomenclatureItemResponseDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class SpecificationResponseDto {

    @JsonProperty("odata.metadata")
    private String metadata;

    @JsonProperty("value")
    private List<SpecificationItemResponseDto> value;

}
