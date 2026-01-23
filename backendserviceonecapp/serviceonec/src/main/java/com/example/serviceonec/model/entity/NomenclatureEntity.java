package com.example.serviceonec.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "nomenclature")
public class NomenclatureEntity extends BaseEntity {

    @Column(name = "ref_key", nullable = false, unique = true)
    private UUID refKey;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "code", nullable = false, unique = true)
    private String code;

}
