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

    @JsonProperty("Цена")
    private Double price;

    @JsonProperty("Вес")
    private Double weight;

    @JsonProperty("Количество")
    private Double quantity;

    @JsonProperty("Длина")
    private Double length;

    @JsonProperty("ДатаПартии")
    private LocalDateTime batchDate;

}
