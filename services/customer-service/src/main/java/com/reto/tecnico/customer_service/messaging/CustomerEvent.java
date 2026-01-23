package com.reto.tecnico.customer_service.messaging;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CustomerEvent(
        UUID eventId,
        String eventType,
        OffsetDateTime occurredAt,
        CustomerEventPayload payload
) {
}
