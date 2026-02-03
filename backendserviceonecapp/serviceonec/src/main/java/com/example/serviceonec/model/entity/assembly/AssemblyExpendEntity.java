package com.example.serviceonec.model.entity.assembly;

import com.example.serviceonec.model.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "assembly_expend")
public class AssemblyExpendEntity extends BaseEntity {

    @Column(name = "nomenclature", nullable = false)
    private String nomenclature;

    @Column(name = "nomenclature_key", nullable = false)
    private UUID nomenclatureKey;

    @Column(name = "characteristic")
    private String characteristic;

    @Column(name = "characteristic_key", nullable = false)
    private UUID characteristicKey;

    @Column(name = "date")
    private LocalDateTime date;

    @Column(name = "quantity")
    private BigDecimal quantity;

    @Column(name = "price")
    private BigDecimal price;

    @Column(name = "cost_price")
    private BigDecimal costPrice;

    @Column(name = "expend_stocks_id")
    private Long expendStocksId;

}
