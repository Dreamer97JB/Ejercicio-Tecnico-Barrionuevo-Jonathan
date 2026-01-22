package com.reto.tecnico.account_service.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record CreateAccountRequest(
        @NotBlank String accountNumber,
        @NotBlank String accountType,
        @NotNull @DecimalMin(value = "0.0", inclusive = true) BigDecimal initialBalance,
        @NotNull UUID clienteId
) {
}
