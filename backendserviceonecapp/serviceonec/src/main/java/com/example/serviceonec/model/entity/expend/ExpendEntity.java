package com.example.serviceonec.model.entity.expend;

import com.example.serviceonec.model.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "expends")
public class ExpendEntity extends BaseEntity {

    @Column(name = "date", nullable = false)
    private LocalDateTime date;

    @Column(name = "number", nullable = false)
    private String number;

    @Column(name = "refKey", nullable = false, unique = true)
    private UUID refKey;

    @Column(name = "organizationKey", nullable = false)
    private UUID organizationKey;

    @Column(name = "doc_order")
    private UUID docOrder;

    @Column(name = "structural_unit_key", nullable = false)
    private UUID structuralUnitKey;

    @Column(name = "operationType", nullable = false)
    private String operationType;

}
