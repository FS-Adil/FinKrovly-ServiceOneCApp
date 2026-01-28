package com.example.serviceonec.model.entity.production;

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
@Table(name = "productions")
public class ProductionEntity extends BaseEntity {

    @Column(name = "ref_key", nullable = false, unique = true)
    private UUID refKey;

    @Column(name = "document_number", nullable = false)
    private String number;

    @Column(name = "document_date", nullable = false)
    private LocalDateTime date;

    @Column(name = "organization_key", nullable = false)
    private UUID organizationKey;

    @Column(name = "customer_order_key")
    private UUID customerOrderKey;

}
