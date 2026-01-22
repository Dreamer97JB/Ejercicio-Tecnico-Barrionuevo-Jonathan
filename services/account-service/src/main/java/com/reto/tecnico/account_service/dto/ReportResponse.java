package com.reto.tecnico.account_service.dto;

import java.util.List;
import java.util.UUID;

public record ReportResponse(
        UUID clienteId,
        String identificacion,
        String name,
        List<ReportAccountResponse> accounts
) {
}
