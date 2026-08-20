package com.example.serviceonec.controller.costprice.output;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
public class CostPriceAutoFillingControllerOutput {

    private String refKey;
    private String number;
    private String name;
    private String characteristic;
    private String batch;
    private String measurementUnit;
    private String categories;
    private BigDecimal quantity;
    private BigDecimal price;
    private BigDecimal cost;
    private String autoFilling;

}
