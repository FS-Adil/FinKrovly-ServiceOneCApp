package com.example.serviceonec.model.entity.expend;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "expend_full")
public class ExpendFullEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "date", nullable = false)
    private LocalDateTime date;

    @Column(name = "document_type", nullable = false)
    private String documentType;

    @Column(name = "document_type_one_c", nullable = false)
    private String documentTypeOneC;

    @Column(name = "number", nullable = false)
    private String number;

    @Column(name = "ref_key", nullable = false, unique = true)
    private UUID refKey;

    @Column(name = "organization_key", nullable = false)
    private UUID organizationKey;

    @Column(name = "structural_unit_key", nullable = false)
    private UUID structuralUnitKey;

    @Column(name = "customer_order", nullable = false)
    private UUID customerOrder;

    @Column(name = "nomenclature_key", nullable = false)
    private UUID nomenclatureKey;

    @Column(name = "characteristic_key", nullable = false)
    private UUID characteristicKey;

    @Column(name = "batch_key", nullable = false)
    private UUID batchKey;

    @Column(name = "quantity", nullable = false)
    private BigDecimal quantity;

    @Column(name = "price", nullable = false)
    private BigDecimal price;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
