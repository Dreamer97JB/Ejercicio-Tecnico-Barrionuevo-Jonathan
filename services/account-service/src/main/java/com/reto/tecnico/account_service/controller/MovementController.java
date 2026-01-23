package com.reto.tecnico.account_service.controller;

import com.reto.tecnico.account_service.dto.CreateMovementRequest;
import com.reto.tecnico.account_service.dto.MovementResponse;
import com.reto.tecnico.account_service.dto.RectifyMovementResponse;
import com.reto.tecnico.account_service.dto.UpdateMovementRequest;
import com.reto.tecnico.account_service.dto.VoidMovementRequest;
import com.reto.tecnico.account_service.dto.VoidMovementResponse;
import com.reto.tecnico.account_service.service.MovementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
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
@RequestMapping("/movimientos")
@RequiredArgsConstructor
@Tag(name = "Movement")
public class MovementController {

    private final MovementService movementService;

    @PostMapping
    @Operation(summary = "Create movement", description = "Registers a new movement and updates account balance.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Movement created"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "404", description = "Account not found"),
            @ApiResponse(responseCode = "409", description = "Business rule conflict")
    })
    public ResponseEntity<MovementResponse> create(@Valid @RequestBody CreateMovementRequest request) {
        MovementResponse response = movementService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(
            summary = "List movements",
            description = "Filters movements by account, customer, and date range. By default only ACTIVE movements are returned."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Movements list"),
            @ApiResponse(responseCode = "400", description = "Invalid filters")
    })
    public List<MovementResponse> search(
            @RequestParam(required = false) String accountNumber,
            @RequestParam(required = false) UUID clienteId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(defaultValue = "false") boolean includeVoided
    ) {
        return movementService.search(accountNumber, clienteId, fechaDesde, fechaHasta, includeVoided);
    }

    @GetMapping("/{movementId}")
    @Operation(
            summary = "Get movement by id",
            description = "Returns the movement including status and traceability metadata."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Movement details"),
            @ApiResponse(responseCode = "404", description = "Movement not found")
    })
    public MovementResponse getById(@PathVariable UUID movementId) {
        return movementService.getById(movementId);
    }

    @PutMapping("/{movementId}")
    @Operation(
            summary = "Rectify movement",
            description = "Creates a reversal and a replacement movement. The original is marked SUPERSEDED and balances are reconciled."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Movement rectified"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "404", description = "Movement not found"),
            @ApiResponse(responseCode = "409", description = "Movement not active"),
            @ApiResponse(responseCode = "422", description = "Reconciliation would result in negative balance")
    })
    public RectifyMovementResponse rectify(
            @PathVariable UUID movementId,
            @Valid @RequestBody UpdateMovementRequest request
    ) {
        return movementService.rectify(movementId, request);
    }

    @DeleteMapping("/{movementId}")
    @Operation(
            summary = "Void movement",
            description = "Voids a movement by creating a reversal and reconciling balances."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Movement voided"),
            @ApiResponse(responseCode = "404", description = "Movement not found"),
            @ApiResponse(responseCode = "409", description = "Movement not active"),
            @ApiResponse(responseCode = "422", description = "Reconciliation would result in negative balance")
    })
    public ResponseEntity<VoidMovementResponse> voidMovement(
            @PathVariable UUID movementId,
            @Valid @RequestBody(required = false) VoidMovementRequest request
    ) {
        String reason = request != null ? request.reason() : null;
        VoidMovementResponse response = movementService.voidMovement(movementId, reason);
        return ResponseEntity.ok(response);
    }
}
