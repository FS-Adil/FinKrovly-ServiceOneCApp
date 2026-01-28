package com.example.serviceonec.model.dto.response.production;

import com.example.serviceonec.model.dto.response.inventory.InventoryItemResponseDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class ProductionResponseDto {

    @JsonProperty("value")
    private List<ProductionItemResponseDto> value;

    @JsonProperty("odata.metadata")
    private String metadata;

}
