package com.reto.tecnico.account_service.dto;

import java.math.BigDecimal;
import java.util.List;

public record ReportAccountResponse(
        String accountNumber,
        String accountType,
        BigDecimal initialBalance,
        BigDecimal currentBalance,
        List<MovementResponse> movements
) {
}
