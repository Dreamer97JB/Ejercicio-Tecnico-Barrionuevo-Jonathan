package com.reto.tecnico.customer_service.dto;

import java.util.UUID;

public record CustomerResponse(
        UUID clienteId,
        String name,
        String gender,
        int age,
        String identificacion,
        String tipoIdentificacion,
        String address,
        String phone,
        boolean active
) {
}
