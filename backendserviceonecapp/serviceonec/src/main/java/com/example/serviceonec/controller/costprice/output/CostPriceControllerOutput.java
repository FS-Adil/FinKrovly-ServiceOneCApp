package com.example.serviceonec.controller.costprice.output;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

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
    private Double quantity;
    private Double price;
    private Double cost;

}
