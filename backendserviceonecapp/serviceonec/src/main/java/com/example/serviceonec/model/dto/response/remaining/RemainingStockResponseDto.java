package com.example.serviceonec.model.dto.response.remaining;

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
