package com.reto.tecnico.account_service.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountResponse(
        String accountNumber,
        String accountType,
        BigDecimal initialBalance,
        BigDecimal currentBalance,
        boolean active,
        UUID clienteId
) {
}
