package com.example.serviceonec.model.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class MeasurementUnitResponseDto {

    @JsonProperty("odata.metadata")
    private String metadata;

    @JsonProperty("value")
    private List<MeasurementUnitItemResponseDto> value;

}
