package com.example.serviceonec.model.entity.production;

import com.example.serviceonec.model.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "production_items")
public class ProductionItemsEntity extends BaseEntity {

    @Column(name = "ref_key", nullable = false)
    private UUID refKey;

    @Column(name = "line_number", nullable = false)
    private String lineNumber;

    @Column(name = "nomenclature_key")
    private UUID nomenclatureKey;

    @Column(name = "characteristic_key")
    private UUID characteristicKey;

    @Column(name = "batch_key")
    private UUID batchKey;

    @Column(name = "quantity")
    private BigDecimal quantity;

    @Column(name = "price")
    private BigDecimal price;

    @Column(name = "specification_key")
    private UUID specificationKey;

    @Column(name = "structural_unit_key", nullable = false)
    private UUID structuralUnitKey;

}
