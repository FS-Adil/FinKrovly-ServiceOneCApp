package com.example.serviceonec.controller.assembly.output;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
public class AssemblyExpendControllerOutput {

    private String name;
    private Double quantity;
    private Double price;
    private Double cost;

}
