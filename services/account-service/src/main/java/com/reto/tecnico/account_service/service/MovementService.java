package com.reto.tecnico.account_service.service;

import com.reto.tecnico.account_service.dto.CreateMovementRequest;
import com.reto.tecnico.account_service.dto.MovementResponse;
import com.reto.tecnico.account_service.entity.Account;
import com.reto.tecnico.account_service.entity.Movement;
import com.reto.tecnico.account_service.entity.MovementType;
import com.reto.tecnico.account_service.exception.ConflictException;
import com.reto.tecnico.account_service.exception.NotFoundException;
import com.reto.tecnico.account_service.repository.AccountRepository;
import com.reto.tecnico.account_service.repository.MovementRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
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

        Movement saved = movementRepository.save(movement);
        return new MovementResponse(
                saved.getMovementId(),
                saved.getAccountNumber(),
                saved.getMovementType(),
                saved.getAmount(),
                saved.getBalanceAfter(),
                saved.getMovementDate()
        );
    }
}
