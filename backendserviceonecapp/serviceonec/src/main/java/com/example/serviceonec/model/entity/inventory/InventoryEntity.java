package com.example.serviceonec.model.entity.inventory;

import com.example.serviceonec.model.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "inventories")
public class InventoryEntity extends BaseEntity {

    @Column(name = "ref_key", nullable = false, unique = true)
    private UUID refKey;

    @Column(name = "number", nullable = false)
    private String number;

    @Column(name = "date", nullable = false)
    private LocalDateTime date;

    @Column(name = "organization_key", nullable = false)
    private UUID organizationKey;

    @Column(name = "structural_unit_key", nullable = false)
    private UUID structuralUnitKey;

//    @OneToMany(mappedBy = "inventoryItem", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
//    private List<InventoryStocksEntity> stocks = new ArrayList<>();

}
