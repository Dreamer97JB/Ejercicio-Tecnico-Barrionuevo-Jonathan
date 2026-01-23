package com.reto.tecnico.account_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record VoidMovementRequest(
        @Schema(description = "Reason for voiding the movement", example = "Customer request", nullable = true)
        @Size(max = 255) String reason
) {
}
