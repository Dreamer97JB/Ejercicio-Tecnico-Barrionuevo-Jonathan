package com.reto.tecnico.account_service.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateAccountRequest(
        @NotBlank String accountType
) {
}
