package com.example.serviceonec.model.dto.response.inventory;

import com.example.serviceonec.model.dto.response.expend.ExpendItemResponseDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class InventoryResponseDto {

    @JsonProperty("value")
    private List<InventoryItemResponseDto> value;

    @JsonProperty("odata.metadata")
    private String metadata;

}
