package com.example.serviceonec.controller.inventorycost.input;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class InventoryCostControllerInput {

    @JsonProperty("date")
    private LocalDateTime dateTo;

    @JsonProperty("organizationId")
    private UUID organizationId;

}
