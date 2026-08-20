package com.example.serviceonec.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.util.UUID;


@Getter
@Setter
@Entity
@Table(name = "nomenclature")
public class NomenclatureEntity extends BaseEntity {

    @Column(name = "ref_key", nullable = false, unique = true)
    private UUID refKey;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "measurement_unit_key")
    private UUID measurementUnitKey;

    @Column(name = "categories_key")
    private UUID categoriesKey;

    @Column(name = "code", nullable = false, unique = true)
    private String code;

}
