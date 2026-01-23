package com.example.serviceonec.model.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class InvoiceValueResponseDto {

    @JsonProperty("Ref_Key")
    private UUID refKey;

    @JsonProperty("Number")
    private String number;

    @JsonProperty("Организация_Key")
    private UUID organizationKey;

//    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonProperty("Date")
    private LocalDateTime date;

    @JsonProperty("ВидОперации")
    private String operationType;

}
