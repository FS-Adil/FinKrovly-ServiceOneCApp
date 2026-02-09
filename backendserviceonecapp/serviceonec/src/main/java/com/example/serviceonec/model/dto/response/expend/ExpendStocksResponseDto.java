package com.example.serviceonec.model.dto.response.expend;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class ExpendStocksResponseDto {
    @JsonProperty("odata.metadata")
    private String metadata;

    @JsonProperty("value")
    private List<ExpendStocksItemResponseDto> value;
}
