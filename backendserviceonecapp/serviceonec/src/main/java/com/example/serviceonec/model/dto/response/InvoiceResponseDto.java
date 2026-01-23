package com.example.serviceonec.model.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class InvoiceResponseDto {

    @JsonProperty("value")
    private List<InvoiceValueResponseDto> value;

    @JsonProperty("odata.metadata")
    private String metadata;
}
