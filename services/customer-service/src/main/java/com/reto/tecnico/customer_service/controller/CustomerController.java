package com.reto.tecnico.customer_service.controller;

import com.reto.tecnico.customer_service.dto.CreateCustomerRequest;
import com.reto.tecnico.customer_service.dto.CustomerResponse;
import com.reto.tecnico.customer_service.dto.UpdateCustomerRequest;
import com.reto.tecnico.customer_service.service.CustomerService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
@RequestMapping("/clientes")
@RequiredArgsConstructor
@Tag(name = "Customer")
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping
    public ResponseEntity<CustomerResponse> create(@Valid @RequestBody CreateCustomerRequest request) {
        CustomerResponse response = customerService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{clienteId}")
    public CustomerResponse getById(@PathVariable UUID clienteId) {
        return customerService.getById(clienteId);
    }

    @GetMapping(params = "identificacion")
    public CustomerResponse getByIdentificacion(@RequestParam String identificacion) {
        return customerService.getByIdentificacion(identificacion);
    }

    @PutMapping("/{clienteId}")
    public CustomerResponse update(
            @PathVariable UUID clienteId,
            @Valid @RequestBody UpdateCustomerRequest request
    ) {
        return customerService.update(clienteId, request);
    }

    @DeleteMapping("/{clienteId}")
    public ResponseEntity<Void> deactivate(@PathVariable UUID clienteId) {
        customerService.deactivate(clienteId);
        return ResponseEntity.noContent().build();
    }
}
