package com.reto.tecnico.account_service.repository;

import com.reto.tecnico.account_service.entity.Movement;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovementRepository extends JpaRepository<Movement, UUID> {

    List<Movement> findByAccountNumberAndMovementDateBetweenOrderByMovementDateAsc(
            String accountNumber,
            OffsetDateTime start,
            OffsetDateTime end
    );
}
