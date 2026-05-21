package com.example.serviceonec.Mobile.dto.output;

import com.example.serviceonec.model.dto.response.rolllist.RollListItemResponseDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class RollMobileOutputResponseDto {
    @JsonProperty("value")
    private List<RollMobileValueOutputResponseDto> value;

    @JsonProperty("odata.metadata")
    private String metadata;
}
