package com.reto.tecnico.customer_service.repository;

import com.reto.tecnico.customer_service.entity.Customer;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    Optional<Customer> findByIdentificacion(String identificacion);

    boolean existsByIdentificacion(String identificacion);

    boolean existsByIdentificacionAndClienteIdNot(String identificacion, UUID clienteId);
}
