package com.example.serviceonec.model.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class CharacteristicResponseDto {

    @JsonProperty("odata.metadata")
    private String odataMetadata;

    private List<CharacteristicItemResponseDto> value;
}
