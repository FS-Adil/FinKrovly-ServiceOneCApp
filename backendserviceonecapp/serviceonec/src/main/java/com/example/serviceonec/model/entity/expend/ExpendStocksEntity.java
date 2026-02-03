package com.example.serviceonec.model.entity.expend;

import com.example.serviceonec.model.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "expend_stocks")
public class ExpendStocksEntity extends BaseEntity {

    @Column(name = "ref_key", nullable = false)
    private UUID refKey;

    @Column(name = "line_number")
    private String lineNumber;

    @Column(name = "nomenclature_key", nullable = false)
    private UUID nomenclatureKey;

    @Column(name = "characteristic_key", nullable = false)
    private UUID characteristicKey;

    @Column(name = "quantity", nullable = false)
    private BigDecimal quantity;

    @Column(name = "measurement_unit")
    private UUID measurementUnit;

    @Column(name = "price", nullable = false)
    private BigDecimal price;
}
