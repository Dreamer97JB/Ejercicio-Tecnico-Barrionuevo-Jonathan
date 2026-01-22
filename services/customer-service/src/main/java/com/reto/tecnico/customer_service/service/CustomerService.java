package com.reto.tecnico.customer_service.service;

import com.reto.tecnico.customer_service.dto.CreateCustomerRequest;
import com.reto.tecnico.customer_service.dto.CustomerResponse;
import com.reto.tecnico.customer_service.dto.UpdateCustomerRequest;
import com.reto.tecnico.customer_service.entity.Customer;
import com.reto.tecnico.customer_service.exception.ConflictException;
import com.reto.tecnico.customer_service.exception.NotFoundException;
import com.reto.tecnico.customer_service.messaging.CustomerEventPublisher;
import com.reto.tecnico.customer_service.repository.CustomerRepository;
import com.reto.tecnico.customer_service.security.PasswordHasher;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final PasswordHasher passwordHasher;
    private final CustomerEventPublisher eventPublisher;

    @Transactional
    public CustomerResponse create(CreateCustomerRequest request) {
        if (customerRepository.existsByIdentificacion(request.identificacion())) {
            throw new ConflictException("Identificacion already exists");
        }

        Customer customer = new Customer();
        customer.setClienteId(UUID.randomUUID());
        customer.setName(request.name());
        customer.setGender(request.gender());
        customer.setAge(request.age());
        customer.setIdentificacion(request.identificacion());
        customer.setTipoIdentificacion(request.tipoIdentificacion());
        customer.setAddress(request.address());
        customer.setPhone(request.phone());
        customer.setPasswordHash(passwordHasher.hash(request.password()));
        customer.setActive(true);

        Customer saved = customerRepository.save(customer);
        eventPublisher.publishCreated(saved);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public CustomerResponse getById(UUID clienteId) {
        Customer customer = customerRepository.findById(clienteId)
                .orElseThrow(() -> new NotFoundException("Customer not found"));
        return toResponse(customer);
    }

    @Transactional(readOnly = true)
    public CustomerResponse getByIdentificacion(String identificacion) {
        Customer customer = customerRepository.findByIdentificacion(identificacion)
                .orElseThrow(() -> new NotFoundException("Customer not found"));
        return toResponse(customer);
    }

    @Transactional
    public CustomerResponse update(UUID clienteId, UpdateCustomerRequest request) {
        Customer customer = customerRepository.findById(clienteId)
                .orElseThrow(() -> new NotFoundException("Customer not found"));

        if (customerRepository.existsByIdentificacionAndClienteIdNot(request.identificacion(), clienteId)) {
            throw new ConflictException("Identificacion already exists");
        }

        customer.setName(request.name());
        customer.setGender(request.gender());
        customer.setAge(request.age());
        customer.setIdentificacion(request.identificacion());
        customer.setTipoIdentificacion(request.tipoIdentificacion());
        customer.setAddress(request.address());
        customer.setPhone(request.phone());
        if (request.password() != null) {
            customer.setPasswordHash(passwordHasher.hash(request.password()));
        }

        Customer saved = customerRepository.save(customer);
        eventPublisher.publishUpdated(saved);
        return toResponse(saved);
    }

    @Transactional
    public void deactivate(UUID clienteId) {
        Customer customer = customerRepository.findById(clienteId)
                .orElseThrow(() -> new NotFoundException("Customer not found"));

        customer.setActive(false);
        Customer saved = customerRepository.save(customer);
        eventPublisher.publishDeactivated(saved);
    }

    private CustomerResponse toResponse(Customer customer) {
        return new CustomerResponse(
                customer.getClienteId(),
                customer.getName(),
                customer.getGender(),
                customer.getAge(),
                customer.getIdentificacion(),
                customer.getTipoIdentificacion(),
                customer.getAddress(),
                customer.getPhone(),
                customer.isActive()
        );
    }
}
