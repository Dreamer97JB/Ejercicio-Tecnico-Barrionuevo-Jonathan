package com.reto.tecnico.account_service.controller;

import com.reto.tecnico.account_service.dto.AccountResponse;
import com.reto.tecnico.account_service.dto.CreateAccountRequest;
import com.reto.tecnico.account_service.dto.UpdateAccountRequest;
import com.reto.tecnico.account_service.service.AccountService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cuentas")
@RequiredArgsConstructor
@Tag(name = "Account")
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<AccountResponse> create(@Valid @RequestBody CreateAccountRequest request) {
        AccountResponse response = accountService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{accountNumber}")
    public AccountResponse getByAccountNumber(@PathVariable String accountNumber) {
        return accountService.getByAccountNumber(accountNumber);
    }

    @GetMapping
    public List<AccountResponse> getAll(@RequestParam(required = false) UUID clienteId) {
        return accountService.getAll(clienteId);
    }

    @PutMapping("/{accountNumber}")
    public AccountResponse update(
            @PathVariable String accountNumber,
            @Valid @RequestBody UpdateAccountRequest request
    ) {
        return accountService.update(accountNumber, request);
    }

    @DeleteMapping("/{accountNumber}")
    public ResponseEntity<Void> deactivate(@PathVariable String accountNumber) {
        accountService.deactivate(accountNumber);
        return ResponseEntity.noContent().build();
    }
}
