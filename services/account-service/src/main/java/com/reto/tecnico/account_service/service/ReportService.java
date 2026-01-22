package com.reto.tecnico.account_service.service;

import com.reto.tecnico.account_service.dto.MovementResponse;
import com.reto.tecnico.account_service.dto.ReportAccountResponse;
import com.reto.tecnico.account_service.dto.ReportResponse;
import com.reto.tecnico.account_service.entity.Account;
import com.reto.tecnico.account_service.entity.ClientSnapshot;
import com.reto.tecnico.account_service.entity.Movement;
import com.reto.tecnico.account_service.entity.MovementStatus;
import com.reto.tecnico.account_service.exception.NotFoundException;
import com.reto.tecnico.account_service.repository.AccountRepository;
import com.reto.tecnico.account_service.repository.ClientSnapshotRepository;
import com.reto.tecnico.account_service.repository.MovementRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ClientSnapshotRepository clientSnapshotRepository;
    private final AccountRepository accountRepository;
    private final MovementRepository movementRepository;

    @Transactional(readOnly = true)
    public ReportResponse getReport(LocalDate fechaDesde, LocalDate fechaHasta, UUID clienteId, String identificacion) {
        if (fechaDesde == null || fechaHasta == null) {
            throw new IllegalArgumentException("fechaDesde and fechaHasta are required");
        }
        if (fechaDesde.isAfter(fechaHasta)) {
            throw new IllegalArgumentException("fechaDesde must be before or equal to fechaHasta");
        }

        ClientSnapshot snapshot = resolveSnapshot(clienteId, identificacion);

        OffsetDateTime start = fechaDesde.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime end = fechaHasta.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC).minusNanos(1);

        List<Account> accounts = accountRepository.findByClienteId(snapshot.getClienteId());

        List<ReportAccountResponse> accountResponses = accounts.stream()
                .map(account -> new ReportAccountResponse(
                        account.getAccountNumber(),
                        account.getAccountType(),
                        account.getInitialBalance(),
                        account.getCurrentBalance(),
                        toMovementResponses(account.getAccountNumber(), start, end)
                ))
                .toList();

        return new ReportResponse(
                snapshot.getClienteId(),
                snapshot.getIdentificacion(),
                snapshot.getName(),
                accountResponses
        );
    }

    private ClientSnapshot resolveSnapshot(UUID clienteId, String identificacion) {
        if (clienteId != null) {
            return clientSnapshotRepository.findById(clienteId)
                    .orElseThrow(() -> new NotFoundException("Customer not found"));
        }
        if (identificacion == null || identificacion.isBlank()) {
            throw new IllegalArgumentException("clienteId or identificacion is required");
        }
        return clientSnapshotRepository.findByIdentificacion(identificacion)
                .orElseThrow(() -> new NotFoundException("Customer not found"));
    }

    private List<MovementResponse> toMovementResponses(String accountNumber, OffsetDateTime start, OffsetDateTime end) {
        List<Movement> movements = movementRepository
                .findByAccountNumberAndStatusAndMovementDateBetweenOrderByMovementDateAscCreatedAtAscMovementIdAsc(
                        accountNumber,
                        MovementStatus.ACTIVE,
                        start,
                        end
                );
        return movements.stream()
                .map(this::toMovementResponse)
                .toList();
    }

    private MovementResponse toMovementResponse(Movement movement) {
        return new MovementResponse(
                movement.getMovementId(),
                movement.getAccountNumber(),
                movement.getMovementType(),
                movement.getAmount(),
                movement.getBalanceAfter(),
                movement.getMovementDate(),
                movement.getStatus(),
                movement.getVoidedAt(),
                movement.getVoidReason(),
                movement.getReversalMovementId(),
                movement.getReplacementMovementId()
        );
    }
}
