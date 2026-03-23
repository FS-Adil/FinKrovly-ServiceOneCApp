package com.example.serviceonec.controller.inventorycost.output;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
public class InventoryCostControllerOutput {
    private String refKey;
    private String name;
    private String characteristic;
    private String batch;
    private BigDecimal quantity;
    private BigDecimal cost;
}
