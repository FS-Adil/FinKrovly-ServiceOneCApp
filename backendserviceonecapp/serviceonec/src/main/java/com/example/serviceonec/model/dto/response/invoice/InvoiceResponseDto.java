package com.example.serviceonec.model.dto.response.invoice;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class InvoiceResponseDto {

    @JsonProperty("value")
    private List<InvoiceItemResponseDto> value;

    @JsonProperty("odata.metadata")
    private String metadata;
}
