package com.example.serviceonec.model.entity.invoice;

import com.example.serviceonec.model.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Setter
@Getter
@Table(name = "invoice_stocks")
public class InvoiceStocksEntity extends BaseEntity {

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
