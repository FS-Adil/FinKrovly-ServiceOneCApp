package com.example.serviceonec.model.entity.inventory;

import com.example.serviceonec.model.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "inventory_stocks")
public class InventoryStocksEntity extends BaseEntity {

    @Column(name = "ref_key", nullable = false)
    private UUID refKey;

    @Column(name = "line_number", nullable = false)
    private String lineNumber;

    @Column(name = "nomenclature_key", nullable = false)
    private UUID nomenclatureKey;

    @Column(name = "batch_key")
    private UUID batchKey;

    @Column(name = "characteristic_key")
    private UUID characteristicKey;

    @Column(name = "quantity")
    private BigDecimal quantity;

    @Column(name = "unit_key")
    private UUID unitKey;

    @Column(name = "price")
    private BigDecimal price;

//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "inventory_ref_key", referencedColumnName = "ref_key")
//    private InventoryEntity inventory;

}
