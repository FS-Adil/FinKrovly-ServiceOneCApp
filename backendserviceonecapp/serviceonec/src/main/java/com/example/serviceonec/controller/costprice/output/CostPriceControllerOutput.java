package com.example.serviceonec.controller.costprice.output;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;


@Getter
@Setter
@Builder
public class CostPriceControllerOutput {

    private String refKey;
    private String number;
    private String name;
    private String characteristic;
    private String batch;
    private BigDecimal quantity;
    private BigDecimal price;
    private BigDecimal cost;

}
