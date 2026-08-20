package com.example.serviceonec.model.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.UUID;

@Data
public class CategoriesItemOutputResponseDto {

    @JsonProperty("Description")
    private String description;

    @JsonProperty("Ref_Key")
    private UUID refKey;

}
