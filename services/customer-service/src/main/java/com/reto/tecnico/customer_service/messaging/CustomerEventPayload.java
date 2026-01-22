package com.reto.tecnico.customer_service.messaging;

import java.util.UUID;

public record CustomerEventPayload(
        UUID clienteId,
        String identificacion,
        String tipoIdentificacion,
        String name,
        boolean active
) {
}
