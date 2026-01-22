package com.reto.tecnico.account_service.messaging;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CustomerEvent(
        UUID eventId,
        String eventType,
        OffsetDateTime occurredAt,
        CustomerEventPayload payload
) {
}
