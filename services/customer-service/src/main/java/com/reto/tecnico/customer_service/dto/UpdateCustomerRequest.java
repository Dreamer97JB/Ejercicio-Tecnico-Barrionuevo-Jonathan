package com.reto.tecnico.customer_service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record UpdateCustomerRequest(
        @NotBlank String name,
        @NotBlank String gender,
        @NotNull @Min(0) Integer age,
        @NotBlank String identificacion,
        String tipoIdentificacion,
        @NotBlank String address,
        @NotBlank String phone,
        @Pattern(regexp = ".*\\S.*", message = "password must not be blank") String password
) {
}
