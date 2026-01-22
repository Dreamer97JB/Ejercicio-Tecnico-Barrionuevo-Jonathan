package com.reto.tecnico.account_service.service;

import com.reto.tecnico.account_service.dto.AccountResponse;
import com.reto.tecnico.account_service.dto.CreateAccountRequest;
import com.reto.tecnico.account_service.dto.UpdateAccountRequest;
import com.reto.tecnico.account_service.entity.Account;
import com.reto.tecnico.account_service.entity.ClientSnapshot;
import com.reto.tecnico.account_service.exception.ConflictException;
import com.reto.tecnico.account_service.exception.NotFoundException;
import com.reto.tecnico.account_service.repository.AccountRepository;
import com.reto.tecnico.account_service.repository.ClientSnapshotRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final ClientSnapshotRepository clientSnapshotRepository;

    @Transactional
    public AccountResponse create(CreateAccountRequest request) {
        if (accountRepository.existsById(request.accountNumber())) {
            throw new ConflictException("Account already exists");
        }

        ClientSnapshot snapshot = clientSnapshotRepository.findById(request.clienteId())
                .orElseThrow(() -> new NotFoundException("Customer not found"));

        if (!snapshot.isActive()) {
            throw new ConflictException("Cliente inactivo");
        }

        Account account = new Account();
        account.setAccountNumber(request.accountNumber());
        account.setAccountType(request.accountType());
        account.setInitialBalance(request.initialBalance());
        account.setCurrentBalance(request.initialBalance());
        account.setActive(true);
        account.setClienteId(request.clienteId());

        Account saved = accountRepository.save(account);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public AccountResponse getByAccountNumber(String accountNumber) {
        Account account = accountRepository.findById(accountNumber)
                .orElseThrow(() -> new NotFoundException("Account not found"));
        return toResponse(account);
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> getAll(UUID clienteId) {
        List<Account> accounts = clienteId == null
                ? accountRepository.findAll()
                : accountRepository.findByClienteId(clienteId);
        return accounts.stream().map(this::toResponse).toList();
    }

    @Transactional
    public AccountResponse update(String accountNumber, UpdateAccountRequest request) {
        Account account = accountRepository.findById(accountNumber)
                .orElseThrow(() -> new NotFoundException("Account not found"));

        account.setAccountType(request.accountType());
        Account saved = accountRepository.save(account);
        return toResponse(saved);
    }

    @Transactional
    public void deactivate(String accountNumber) {
        Account account = accountRepository.findById(accountNumber)
                .orElseThrow(() -> new NotFoundException("Account not found"));

        account.setActive(false);
        accountRepository.save(account);
    }

    private AccountResponse toResponse(Account account) {
        return new AccountResponse(
                account.getAccountNumber(),
                account.getAccountType(),
                account.getInitialBalance(),
                account.getCurrentBalance(),
                account.isActive(),
                account.getClienteId()
        );
    }
}
