package com.example.serviceonec.model.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "structural_unit")
public class StructuralUnitEntity extends BaseEntity{

    @Column(name = "ref_key", nullable = false, unique = true)
    private UUID refKey;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "code", nullable = false, unique = true)
    private String code;

    @Column(name = "structural_unit_type", nullable = false)
    private String structuralUnitType;

    @Column(name = "organization_key", nullable = false)
    private UUID organizationKey;

}
