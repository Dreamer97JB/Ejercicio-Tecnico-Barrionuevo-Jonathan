package com.reto.tecnico.account_service.messaging;

import java.util.UUID;

public record CustomerEventPayload(
        UUID clienteId,
        String identificacion,
        String tipoIdentificacion,
        String name,
        boolean active
) {
}
