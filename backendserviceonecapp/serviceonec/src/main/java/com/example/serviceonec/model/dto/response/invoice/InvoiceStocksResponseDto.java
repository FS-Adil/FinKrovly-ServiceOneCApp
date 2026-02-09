package com.example.serviceonec.model.dto.response.invoice;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class InvoiceStocksResponseDto {

    @JsonProperty("odata.metadata")
    private String metadata;

    @JsonProperty("value")
    private List<InvoiceStocksItemResponseDto> value;
}
