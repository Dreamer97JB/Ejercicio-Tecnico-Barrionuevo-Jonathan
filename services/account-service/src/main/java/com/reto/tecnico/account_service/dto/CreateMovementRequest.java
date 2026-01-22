package com.reto.tecnico.account_service.dto;

import com.reto.tecnico.account_service.entity.MovementType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CreateMovementRequest(
        @NotBlank String accountNumber,
        @NotNull MovementType movementType,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal amount
) {
}
