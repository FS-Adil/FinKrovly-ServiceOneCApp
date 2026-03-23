package com.example.serviceonec.model.entity.remaining;

import com.example.serviceonec.model.entity.BaseEntity;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor  // <-- Добавьте эту аннотацию
@AllArgsConstructor // <-- Добавьте эту аннотацию (нужна для @Builder)
@Table(name = "remaining_stocks")
public class RemainingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "date", nullable = false)
    private LocalDateTime date;

    @Column(name = "organization_key", nullable = false)
    private UUID organizationKey;

    @Column(name = "nomenclature_key", nullable = false)
    private UUID nomenclatureKey;

    @Column(name = "characteristic_key", nullable = false)
    private UUID characteristicKey;

    @Column(name = "batch_key", nullable = false)
    private UUID batchKey;

    @Column(name = "quantity_balance", nullable = false)
    private BigDecimal quantityBalance;

    @Column(name = "amount_balance")
    private BigDecimal amountBalance;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
