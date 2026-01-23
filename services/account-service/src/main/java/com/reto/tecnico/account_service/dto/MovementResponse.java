package com.reto.tecnico.account_service.dto;

import com.reto.tecnico.account_service.entity.MovementStatus;
import com.reto.tecnico.account_service.entity.MovementType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record MovementResponse(
        @Schema(description = "Movement identifier", example = "b1f6c73b-0b9a-4b8f-96d6-2e3a8d2f1b7f")
        UUID movementId,
        @Schema(description = "Account number", example = "ACC-1001")
        String accountNumber,
        @Schema(description = "Movement type", example = "DEPOSITO")
        MovementType movementType,
        @Schema(description = "Movement amount", example = "150.00")
        BigDecimal amount,
        @Schema(description = "Balance after movement", example = "350.00")
        BigDecimal balanceAfter,
        @Schema(description = "Movement date", example = "2026-01-20T10:15:30Z")
        OffsetDateTime movementDate,
        @Schema(description = "Movement status", example = "ACTIVE")
        MovementStatus status,
        @Schema(description = "Voided at timestamp", example = "2026-01-21T10:15:30Z", nullable = true)
        OffsetDateTime voidedAt,
        @Schema(description = "Void reason", example = "Customer request", nullable = true)
        String voidReason,
        @Schema(description = "Reversal movement id", example = "c2b8f15e-0b87-4d4e-9a85-ff5b2d1b6b9e", nullable = true)
        UUID reversalMovementId,
        @Schema(description = "Replacement movement id", example = "a7d1d7c0-4bfe-4c0a-9f1d-1d2d5f9b8c7e", nullable = true)
        UUID replacementMovementId
) {
}
