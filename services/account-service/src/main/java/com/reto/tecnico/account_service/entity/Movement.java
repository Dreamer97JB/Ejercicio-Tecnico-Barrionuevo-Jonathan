package com.reto.tecnico.account_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "movements")
@Getter
@Setter
@NoArgsConstructor
public class Movement {

    @Id
    @Column(name = "movement_id", nullable = false)
    private UUID movementId;

    @Column(name = "account_number", nullable = false)
    private String accountNumber;

    @Column(name = "movement_date", nullable = false)
    private OffsetDateTime movementDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false)
    private MovementType movementType;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "balance_after", nullable = false)
    private BigDecimal balanceAfter;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private MovementStatus status;

    @Column(name = "voided_at")
    private OffsetDateTime voidedAt;

    @Column(name = "void_reason")
    private String voidReason;

    @Column(name = "reversal_movement_id")
    private UUID reversalMovementId;

    @Column(name = "replacement_movement_id")
    private UUID replacementMovementId;

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        if (movementDate == null) {
            movementDate = now;
        }
        if (createdAt == null) {
            createdAt = now;
        }
        if (status == null) {
            status = MovementStatus.ACTIVE;
        }
    }
}
