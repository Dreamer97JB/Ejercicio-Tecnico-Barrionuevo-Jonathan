package com.reto.tecnico.account_service.service;

import com.reto.tecnico.account_service.dto.CreateMovementRequest;
import com.reto.tecnico.account_service.dto.MovementResponse;
import com.reto.tecnico.account_service.dto.RectifyMovementResponse;
import com.reto.tecnico.account_service.dto.UpdateMovementRequest;
import com.reto.tecnico.account_service.dto.VoidMovementResponse;
import com.reto.tecnico.account_service.entity.Account;
import com.reto.tecnico.account_service.entity.Movement;
import com.reto.tecnico.account_service.entity.MovementStatus;
import com.reto.tecnico.account_service.entity.MovementType;
import com.reto.tecnico.account_service.exception.ConflictException;
import com.reto.tecnico.account_service.exception.NotFoundException;
import com.reto.tecnico.account_service.exception.UnprocessableEntityException;
import com.reto.tecnico.account_service.repository.AccountRepository;
import com.reto.tecnico.account_service.repository.MovementRepository;
import java.math.BigDecimal;
import java.time.Clock;
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
public class MovementService {

    private final AccountRepository accountRepository;
    private final MovementRepository movementRepository;
    private final Clock clock;

    @Transactional
    public MovementResponse create(CreateMovementRequest request) {
        Account account = accountRepository.findByAccountNumberForUpdate(request.accountNumber())
                .orElseThrow(() -> new NotFoundException("Account not found"));

        if (!account.isActive()) {
            throw new ConflictException("Account inactive");
        }

        BigDecimal amount = request.amount();
        BigDecimal newBalance;

        if (request.movementType() == MovementType.RETIRO) {
            newBalance = account.getCurrentBalance().subtract(amount);
            if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
                throw new ConflictException("Saldo no disponible");
            }
        } else {
            newBalance = account.getCurrentBalance().add(amount);
        }

        account.setCurrentBalance(newBalance);
        accountRepository.save(account);

        Movement movement = new Movement();
        movement.setMovementId(UUID.randomUUID());
        movement.setAccountNumber(account.getAccountNumber());
        movement.setMovementType(request.movementType());
        movement.setAmount(amount);
        movement.setBalanceAfter(newBalance);
        OffsetDateTime now = OffsetDateTime.now(clock);
        movement.setMovementDate(now);
        movement.setCreatedAt(now);
        movement.setStatus(MovementStatus.ACTIVE);

        Movement saved = movementRepository.save(movement);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public MovementResponse getById(UUID movementId) {
        Movement movement = movementRepository.findById(movementId)
                .orElseThrow(() -> new NotFoundException("Movement not found"));
        return toResponse(movement);
    }

    @Transactional(readOnly = true)
    public List<MovementResponse> search(
            String accountNumber,
            UUID clienteId,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            boolean includeVoided
    ) {
        if (fechaDesde != null && fechaHasta != null && fechaDesde.isAfter(fechaHasta)) {
            throw new IllegalArgumentException("fechaDesde must be before or equal to fechaHasta");
        }

        OffsetDateTime fromDate = fechaDesde != null
                ? fechaDesde.atStartOfDay().atOffset(ZoneOffset.UTC)
                : OffsetDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime toDate = fechaHasta != null
                ? fechaHasta.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC).minusNanos(1)
                : OffsetDateTime.of(9999, 12, 31, 23, 59, 59, 999_999_000, ZoneOffset.UTC);

        return movementRepository.search(accountNumber, clienteId, fromDate, toDate, includeVoided).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public RectifyMovementResponse rectify(UUID movementId, UpdateMovementRequest request) {
        Movement original = movementRepository.findByIdForUpdate(movementId)
                .orElseThrow(() -> new NotFoundException("Movement not found"));
        if (original.getStatus() != MovementStatus.ACTIVE) {
            throw new ConflictException("Movement not active");
        }

        Account account = accountRepository.findByAccountNumberForUpdate(original.getAccountNumber())
                .orElseThrow(() -> new NotFoundException("Account not found"));
        if (!account.isActive()) {
            throw new ConflictException("Account inactive");
        }

        OffsetDateTime now = OffsetDateTime.now(clock);

        original.setStatus(MovementStatus.SUPERSEDED);

        Movement reversal = buildReversal(original, now, account.getCurrentBalance());
        movementRepository.save(reversal);

        Movement replacement = new Movement();
        replacement.setMovementId(UUID.randomUUID());
        replacement.setAccountNumber(original.getAccountNumber());
        replacement.setMovementType(request.movementType());
        replacement.setAmount(request.amount());
        replacement.setMovementDate(request.movementDate() != null ? request.movementDate() : now);
        replacement.setCreatedAt(now);
        replacement.setStatus(MovementStatus.ACTIVE);
        replacement.setBalanceAfter(account.getCurrentBalance());
        movementRepository.save(replacement);

        original.setReversalMovementId(reversal.getMovementId());
        original.setReplacementMovementId(replacement.getMovementId());
        movementRepository.save(original);

        reconcileBalances(account);

        return new RectifyMovementResponse(
                original.getMovementId(),
                reversal.getMovementId(),
                replacement.getMovementId(),
                account.getAccountNumber(),
                account.getCurrentBalance()
        );
    }

    @Transactional
    public VoidMovementResponse voidMovement(UUID movementId, String reason) {
        Movement original = movementRepository.findByIdForUpdate(movementId)
                .orElseThrow(() -> new NotFoundException("Movement not found"));
        if (original.getStatus() != MovementStatus.ACTIVE) {
            throw new ConflictException("Movement not active");
        }

        Account account = accountRepository.findByAccountNumberForUpdate(original.getAccountNumber())
                .orElseThrow(() -> new NotFoundException("Account not found"));
        if (!account.isActive()) {
            throw new ConflictException("Account inactive");
        }

        OffsetDateTime now = OffsetDateTime.now(clock);

        original.setStatus(MovementStatus.VOIDED);
        original.setVoidedAt(now);
        original.setVoidReason(reason);

        Movement reversal = buildReversal(original, now, account.getCurrentBalance());
        movementRepository.save(reversal);

        original.setReversalMovementId(reversal.getMovementId());
        movementRepository.save(original);

        reconcileBalances(account);

        return new VoidMovementResponse(
                original.getMovementId(),
                reversal.getMovementId(),
                account.getCurrentBalance()
        );
    }

    private Movement buildReversal(Movement original, OffsetDateTime now, BigDecimal balanceAfter) {
        Movement reversal = new Movement();
        reversal.setMovementId(UUID.randomUUID());
        reversal.setAccountNumber(original.getAccountNumber());
        reversal.setMovementType(opposite(original.getMovementType()));
        reversal.setAmount(original.getAmount());
        reversal.setMovementDate(now);
        reversal.setCreatedAt(now);
        reversal.setStatus(MovementStatus.ACTIVE);
        reversal.setBalanceAfter(balanceAfter);
        return reversal;
    }

    private void reconcileBalances(Account account) {
        List<Movement> activeMovements = movementRepository
                .findByAccountNumberOrderByMovementDateAscCreatedAtAscMovementIdAsc(
                        account.getAccountNumber()
                );

        BigDecimal balance = account.getInitialBalance();
        for (Movement movement : activeMovements) {
            balance = applyMovement(balance, movement);
            movement.setBalanceAfter(balance);
        }

        account.setCurrentBalance(balance);
        movementRepository.saveAll(activeMovements);
        accountRepository.save(account);
    }

    private BigDecimal applyMovement(BigDecimal currentBalance, Movement movement) {
        BigDecimal next = movement.getMovementType() == MovementType.RETIRO
                ? currentBalance.subtract(movement.getAmount())
                : currentBalance.add(movement.getAmount());
        if (next.compareTo(BigDecimal.ZERO) < 0) {
            throw new UnprocessableEntityException("Balance would be negative after reconciliation");
        }
        return next;
    }

    private MovementType opposite(MovementType type) {
        return type == MovementType.RETIRO ? MovementType.DEPOSITO : MovementType.RETIRO;
    }

    private MovementResponse toResponse(Movement movement) {
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
