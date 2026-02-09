package com.example.serviceonec.model.dto.response.expend;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class ExpendResponseDto {

    @JsonProperty("value")
    private List<ExpendItemResponseDto> value;

    @JsonProperty("odata.metadata")
    private String metadata;
}
