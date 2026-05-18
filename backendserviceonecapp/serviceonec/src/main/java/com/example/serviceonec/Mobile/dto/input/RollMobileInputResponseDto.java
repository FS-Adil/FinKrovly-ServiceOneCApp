package com.example.serviceonec.Mobile.dto.input;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RollMobileInputResponseDto {

    private String name;

    private String manufacturer;

    private String organization;

    private String batch;

    private BigDecimal pricePerSquareMeter;

    private BigDecimal pricePerTon;

    private BigDecimal areaSquareMeters;

    private BigDecimal weightTons;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate batchDate;

}
