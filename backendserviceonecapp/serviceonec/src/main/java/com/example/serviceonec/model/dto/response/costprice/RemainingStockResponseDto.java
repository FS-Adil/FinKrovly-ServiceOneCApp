package com.example.serviceonec.model.dto.response.costprice;

import com.example.serviceonec.model.dto.response.rolllist.RollListItemResponseDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class RemainingStockResponseDto {
    @JsonProperty("value")
    private List<RemainingItemStockResponseDto> value;

    @JsonProperty("odata.metadata")
    private String metadata;
}
