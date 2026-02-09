package com.example.serviceonec.model.dto.response.inventory;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class InventoryItemResponseDto {

    @JsonProperty("Ref_Key")
    private UUID refKey;

    @JsonProperty("Number")
    private String number;

    @JsonProperty("Date")
    private LocalDateTime date;

    @JsonProperty("Организация_Key")
    private UUID organizationKey;

    @JsonProperty("СтруктурнаяЕдиница_Key")
    private UUID structuralUnitKey;

    @JsonProperty("Запасы")
    private List<InventoryStocksResponseDto> stocks;

    @Data
    public static class InventoryStocksResponseDto {

        @JsonProperty("Ref_Key")
        private UUID refKey;

        @JsonProperty("LineNumber")
        private String lineNumber;

        @JsonProperty("Номенклатура_Key")
        private UUID nomenclatureKey;

        @JsonProperty("Характеристика_Key")
        private UUID characteristicKey;

        @JsonProperty("Партия_Key")
        private UUID batchKey;

        @JsonProperty("СерииНоменклатуры")
        private String series;

        @JsonProperty("Количество")
        private Double quantity;

        @JsonProperty("ЕдиницаИзмерения")
        private UUID unitKey;

        @JsonProperty("ЕдиницаИзмерения_Type")
        private String unitType;

        @JsonProperty("Цена")
        private Double price;

        @JsonProperty("Сумма")
        private Double amount;

        @JsonProperty("КлючСвязи")
        private String connectionKey;

        @JsonProperty("СтранаПроисхождения_Key")
        private UUID countryOriginKey;

        @JsonProperty("НомерГТД_Key")
        private UUID gtdNumberKey;

        @JsonProperty("ИдентификаторСтроки")
        private String rowIdentifier;

        @JsonProperty("ПрослеживаемыйТовар")
        private boolean traceableProduct;

        @JsonProperty("ПрослеживаемыйКомплект")
        private boolean traceableKit;
    }
}
