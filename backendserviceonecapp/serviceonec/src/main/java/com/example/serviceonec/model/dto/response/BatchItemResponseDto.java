package com.example.serviceonec.model.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class BatchItemResponseDto {

    @JsonProperty("Ref_Key")
    private UUID refKey;

    @JsonProperty("Description")
    private String description;

    @JsonProperty("Code")
    private String code;

    @JsonProperty("_Цена")
    private Double price;

    @JsonProperty("_Вес")
    private Double weight;

    @JsonProperty("_Количество")
    private Double quantity;

    @JsonProperty("_Длина")
    private Double length;

    @JsonProperty("_ПартияДата")
    private LocalDateTime batchDate;

}
