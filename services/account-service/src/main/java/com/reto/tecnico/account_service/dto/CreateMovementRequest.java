package com.reto.tecnico.account_service.dto;

import com.reto.tecnico.account_service.entity.MovementType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CreateMovementRequest(
        @Schema(description = "Account number", example = "ACC-1001")
        @NotBlank String accountNumber,
        @Schema(description = "Movement type", example = "DEPOSITO")
        @NotNull MovementType movementType,
        @Schema(description = "Movement amount", example = "150.00")
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal amount
) {
}
