package com.example.serviceonec.model.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class BatchResponseDto {

    @JsonProperty("odata.metadata")
    private String metadata;

    @JsonProperty("value")
    private List<BatchItemResponseDto> value;

}
