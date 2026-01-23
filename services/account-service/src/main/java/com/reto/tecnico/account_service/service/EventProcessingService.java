package com.reto.tecnico.account_service.service;

import com.reto.tecnico.account_service.entity.ClientSnapshot;
import com.reto.tecnico.account_service.entity.ProcessedEvent;
import com.reto.tecnico.account_service.messaging.CustomerEvent;
import com.reto.tecnico.account_service.messaging.CustomerEventPayload;
import com.reto.tecnico.account_service.repository.ClientSnapshotRepository;
import com.reto.tecnico.account_service.repository.ProcessedEventRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EventProcessingService {

    private final ClientSnapshotRepository clientSnapshotRepository;
    private final ProcessedEventRepository processedEventRepository;

    @Transactional
    public void process(CustomerEvent event) {
        if (event == null || event.eventId() == null || isBlank(event.eventType()) || event.occurredAt() == null) {
            return;
        }
        CustomerEventPayload payload = event.payload();
        if (payload == null || payload.clienteId() == null || isBlank(payload.identificacion())
                || isBlank(payload.name())) {
            return;
        }

        UUID eventId = event.eventId();
        if (processedEventRepository.existsById(eventId)) {
            return;
        }

        ClientSnapshot snapshot = clientSnapshotRepository.findById(payload.clienteId())
                .orElseGet(() -> {
                    ClientSnapshot created = new ClientSnapshot();
                    created.setClienteId(payload.clienteId());
                    return created;
                });

        snapshot.setIdentificacion(payload.identificacion());
        snapshot.setTipoIdentificacion(payload.tipoIdentificacion());
        snapshot.setName(payload.name());
        snapshot.setActive(payload.active());
        snapshot.setLastEventId(eventId);
        snapshot.setLastEventAt(event.occurredAt());

        clientSnapshotRepository.save(snapshot);

        ProcessedEvent processedEvent = new ProcessedEvent();
        processedEvent.setEventId(eventId);
        processedEventRepository.save(processedEvent);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
