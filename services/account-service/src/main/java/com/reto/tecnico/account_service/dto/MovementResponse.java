package com.reto.tecnico.account_service.dto;

import com.reto.tecnico.account_service.entity.MovementType;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record MovementResponse(
        UUID movementId,
        String accountNumber,
        MovementType movementType,
        BigDecimal amount,
        BigDecimal balanceAfter,
        OffsetDateTime movementDate
) {
}
