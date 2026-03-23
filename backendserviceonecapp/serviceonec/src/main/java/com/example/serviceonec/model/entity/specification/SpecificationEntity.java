package com.example.serviceonec.model.entity.specification;

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
@Table(name = "specification")
public class SpecificationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ref_key", nullable = false)
    private UUID refKey;

    @Column(name = "quantity", nullable = false)
    private BigDecimal quantity;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

}
