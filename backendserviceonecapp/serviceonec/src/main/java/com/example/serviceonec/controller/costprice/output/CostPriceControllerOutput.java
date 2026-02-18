package com.example.serviceonec.controller.costprice.output;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class CostPriceControllerOutput {

    private String name;
    private Double quantity;
    private Double price;
    private Double cost;

}
