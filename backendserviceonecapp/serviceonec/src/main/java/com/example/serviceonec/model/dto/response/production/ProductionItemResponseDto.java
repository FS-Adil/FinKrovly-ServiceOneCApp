package com.example.serviceonec.model.dto.response.production;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class ProductionItemResponseDto {

    @JsonProperty("Организация_Key")
    private UUID organizationKey;

    @JsonProperty("ЗаказПокупателя_Key")
    private UUID customerOrderKey;

    @JsonProperty("Number")
    private String number;

    @JsonProperty("Ref_Key")
    private UUID refKey;

    @JsonProperty("Date")
    private LocalDateTime date;

    @JsonProperty("Запасы")
    private List<ProductionStocksDto> stocks;

    @JsonProperty("Продукция")
    private List<ProductionItemsDto> products;

    @Data
    public static class ProductionStocksDto {

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
        private String nomenclatureSeries;

        @JsonProperty("Количество")
        private Double quantity;

        @JsonProperty("Резерв")
        private Double reserve;

        @JsonProperty("ЕдиницаИзмерения")
        private String unitOfMeasurement;

        @JsonProperty("ЕдиницаИзмерения_Type")
        private String unitOfMeasurementType;

        @JsonProperty("Спецификация_Key")
        private UUID specificationKey;

        @JsonProperty("ДоляСтоимости")
        private Double costShare;

        @JsonProperty("КлючСвязи")
        private String linkKey;

        @JsonProperty("СтранаПроисхождения_Key")
        private UUID countryOfOriginKey;

        @JsonProperty("НомерГТД_Key")
        private UUID gtdNumberKey;

        @JsonProperty("СтруктурнаяЕдиница_Key")
        private UUID structuralUnitKey;

        @JsonProperty("Ячейка_Key")
        private UUID cellKey;

        @JsonProperty("ЗаказПокупателя_Key")
        private UUID orderKey;

        @JsonProperty("Этап_Key")
        private UUID stageKey;

        @JsonProperty("ИдентификаторСтроки")
        private String rowIdentifier;

        @JsonProperty("ПрослеживаемыйТовар")
        private Boolean traceableProduct;

        @JsonProperty("ПрослеживаемыйКомплект")
        private Boolean traceableKit;

        @JsonProperty("УчитыватьВНУ")
        private Boolean accountInNU;
    }

    @Data
    public static class ProductionItemsDto {

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
        private String nomenclatureSeries;

        @JsonProperty("Количество")
        private Integer quantity;

        @JsonProperty("Резерв")
        private Double reserve;

        @JsonProperty("ЕдиницаИзмерения")
        private String unitOfMeasurement;

        @JsonProperty("ЕдиницаИзмерения_Type")
        private String unitOfMeasurementType;

        @JsonProperty("Спецификация_Key")
        private UUID specificationKey;

        @JsonProperty("КлючСвязи")
        private String linkKey;

        @JsonProperty("СтранаПроисхождения_Key")
        private UUID countryOfOriginKey;

        @JsonProperty("НомерГТД_Key")
        private UUID gtdNumberKey;

        @JsonProperty("СтруктурнаяЕдиница_Key")
        private UUID structuralUnitKey;

        @JsonProperty("Ячейка_Key")
        private UUID cellKey;

        @JsonProperty("ЗаказПокупателя_Key")
        private UUID orderKey;

        @JsonProperty("ПодразделениеЗавершающегоЭтапа_Key")
        private UUID finalStageDepartmentKey;

        @JsonProperty("ИдентификаторСтроки")
        private String rowIdentifier;

        @JsonProperty("ПрослеживаемыйТовар")
        private Boolean traceableProduct;

        @JsonProperty("ПрослеживаемыйКомплект")
        private Boolean traceableKit;

        @JsonProperty("_Длина")
        private Double length;

        @JsonProperty("_Штук")
        private String pieces;
    }
}
