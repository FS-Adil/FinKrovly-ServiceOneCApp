package com.example.serviceonec.model.dto.response.rolllist;

import com.example.serviceonec.model.dto.response.production.ProductionItemResponseDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class RollListResponseDto {

    @JsonProperty("value")
    private List<RollListItemResponseDto> value;

    @JsonProperty("odata.metadata")
    private String metadata;

}
