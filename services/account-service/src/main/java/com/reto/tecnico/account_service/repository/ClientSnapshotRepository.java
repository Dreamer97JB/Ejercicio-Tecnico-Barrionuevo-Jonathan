package com.reto.tecnico.account_service.repository;

import com.reto.tecnico.account_service.entity.ClientSnapshot;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientSnapshotRepository extends JpaRepository<ClientSnapshot, UUID> {

    Optional<ClientSnapshot> findByIdentificacion(String identificacion);
}
