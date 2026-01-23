package com.reto.tecnico.account_service.repository;

import com.reto.tecnico.account_service.entity.Movement;
import com.reto.tecnico.account_service.entity.MovementStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface MovementRepository extends JpaRepository<Movement, UUID> {

    List<Movement> findByAccountNumberAndStatusAndMovementDateBetweenOrderByMovementDateAscCreatedAtAscMovementIdAsc(
            String accountNumber,
            MovementStatus status,
            OffsetDateTime start,
            OffsetDateTime end
    );

    List<Movement> findByAccountNumberAndStatusOrderByMovementDateAscCreatedAtAscMovementIdAsc(
            String accountNumber,
            MovementStatus status
    );

    List<Movement> findByAccountNumberOrderByMovementDateAscCreatedAtAscMovementIdAsc(
            String accountNumber
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Movement m where m.movementId = :movementId")
    java.util.Optional<Movement> findByIdForUpdate(@Param("movementId") UUID movementId);

    @Query("""
            select m from Movement m
            join Account a on a.accountNumber = m.accountNumber
            where (:accountNumber is null or m.accountNumber = :accountNumber)
              and (:clienteId is null or a.clienteId = :clienteId)
              and (m.movementDate between :fromDate and :toDate)
              and (:includeVoided = true or m.status = com.reto.tecnico.account_service.entity.MovementStatus.ACTIVE)
            order by m.movementDate asc, m.createdAt asc, m.movementId asc
            """)
    List<Movement> search(
            @Param("accountNumber") String accountNumber,
            @Param("clienteId") UUID clienteId,
            @Param("fromDate") OffsetDateTime fromDate,
            @Param("toDate") OffsetDateTime toDate,
            @Param("includeVoided") boolean includeVoided
    );
}
