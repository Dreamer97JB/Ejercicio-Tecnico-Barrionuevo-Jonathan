package com.reto.tecnico.account_service.dto;

import com.reto.tecnico.account_service.entity.MovementType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record UpdateMovementRequest(
        @Schema(description = "Movement type", example = "RETIRO")
        @NotNull MovementType movementType,
        @Schema(description = "Movement amount", example = "75.00")
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal amount,
        @Schema(description = "Movement date (optional)", example = "2026-01-20T10:15:30Z", nullable = true)
        OffsetDateTime movementDate
) {
}
