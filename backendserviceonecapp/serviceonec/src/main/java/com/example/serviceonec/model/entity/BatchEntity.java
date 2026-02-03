package com.example.serviceonec.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "batches")
public class BatchEntity extends BaseEntity{

    @Column(name = "ref_key", nullable = false, unique = true)
    private UUID refKey;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "code", length = 50, unique = true)
    private String code;

    @Column(name = "price")
    private BigDecimal price;

    @Column(name = "weight")
    private BigDecimal weight;

    @Column(name = "quantity")
    private BigDecimal quantity;

    @Column(name = "length")
    private BigDecimal length;

    @Column(name = "batch_date")
    private LocalDateTime batchDate;
}
