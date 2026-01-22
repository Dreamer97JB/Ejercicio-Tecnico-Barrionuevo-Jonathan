package com.reto.tecnico.account_service.controller;

import com.reto.tecnico.account_service.dto.CreateMovementRequest;
import com.reto.tecnico.account_service.dto.MovementResponse;
import com.reto.tecnico.account_service.service.MovementService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/movimientos")
@RequiredArgsConstructor
@Tag(name = "Movement")
public class MovementController {

    private final MovementService movementService;

    @PostMapping
    public ResponseEntity<MovementResponse> create(@Valid @RequestBody CreateMovementRequest request) {
        MovementResponse response = movementService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
