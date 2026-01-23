package com.reto.tecnico.account_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "client_snapshot")
@Getter
@Setter
@NoArgsConstructor
public class ClientSnapshot {

    @Id
    @Column(name = "cliente_id", nullable = false)
    private UUID clienteId;

    @Column(name = "identificacion", nullable = false, unique = true)
    private String identificacion;

    @Column(name = "tipo_identificacion")
    private String tipoIdentificacion;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "last_event_id")
    private UUID lastEventId;

    @Column(name = "last_event_at")
    private OffsetDateTime lastEventAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void onCreate() {
        updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
}
