package com.reto.tecnico.account_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.UUID;

public record RectifyMovementResponse(
        @Schema(description = "Original movement id", example = "b1f6c73b-0b9a-4b8f-96d6-2e3a8d2f1b7f")
        UUID originalMovementId,
        @Schema(description = "Reversal movement id", example = "c2b8f15e-0b87-4d4e-9a85-ff5b2d1b6b9e")
        UUID reversalMovementId,
        @Schema(description = "Replacement movement id", example = "a7d1d7c0-4bfe-4c0a-9f1d-1d2d5f9b8c7e")
        UUID replacementMovementId,
        @Schema(description = "Account number", example = "ACC-1001")
        String accountNumber,
        @Schema(description = "New current balance", example = "250.00")
        BigDecimal newCurrentBalance
) {
}
