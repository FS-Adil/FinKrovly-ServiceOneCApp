package com.example.serviceonec.controller.assembly.input;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class AssemblyExpendControllerInput {

    @JsonProperty("startDate")
    private LocalDateTime dateFrom;

    @JsonProperty("endDate")
    private LocalDateTime dateTo;

    private UUID organizationId;

}
