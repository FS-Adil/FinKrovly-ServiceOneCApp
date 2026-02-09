package com.example.serviceonec.model.entity.rolllist;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
public class RollListEntity {

    private String nomenclatureName;

    private String characteristicName;

    private String batchName;

    private Double quantityBalance;

    private BigDecimal weight;

    private BigDecimal length;

    private String location;

}
