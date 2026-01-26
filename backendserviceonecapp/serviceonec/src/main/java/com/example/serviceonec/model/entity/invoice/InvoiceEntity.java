package com.example.serviceonec.model.entity.invoice;

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
@Table(name = "invoices")
public class InvoiceEntity extends BaseEntity {

    @Column(name = "date", nullable = false)
    private LocalDateTime date;

    @Column(name = "number", nullable = false)
    private String number;

    @Column(name = "refKey", nullable = false, unique = true)
    private UUID refKey;

    @Column(name = "organizationKey", nullable = false)
    private UUID organizationKey;

    @Column(name = "operationType", nullable = false)
    private String operationType;

//    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
//    private Set<Post> posts = new HashSet<>();
}
