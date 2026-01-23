package com.reto.tecnico.customer_service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.reto.tecnico.customer_service.dto.CreateCustomerRequest;
import com.reto.tecnico.customer_service.entity.Customer;
import com.reto.tecnico.customer_service.messaging.CustomerEventPublisher;
import com.reto.tecnico.customer_service.repository.CustomerRepository;
import com.reto.tecnico.customer_service.security.PasswordHasher;
import com.reto.tecnico.customer_service.service.CustomerService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@ExtendWith(MockitoExtension.class)
class CustomerServiceApplicationTests {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private CustomerEventPublisher eventPublisher;

    private PasswordHasher passwordHasher;
    private CustomerService customerService;
    private Validator validator;

    @BeforeEach
    void setUp() {
        passwordHasher = new PasswordHasher(new BCryptPasswordEncoder());
        customerService = new CustomerService(customerRepository, passwordHasher, eventPublisher);
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void createHashesPasswordAndSetsActive() {
        CreateCustomerRequest request = new CreateCustomerRequest(
                "Test User",
                "M",
                30,
                "ID-123",
                "CC",
                "Street 123",
                "555-111",
                "secret"
        );

        when(customerRepository.existsByIdentificacion(request.identificacion())).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        customerService.create(request);

        ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).save(captor.capture());
        Customer saved = captor.getValue();

        assertThat(saved.isActive()).isTrue();
        assertThat(saved.getPasswordHash()).isNotEqualTo(request.password());
        assertThat(passwordHasher.matches(request.password(), saved.getPasswordHash())).isTrue();
        verify(eventPublisher).publishCreated(saved);
    }

    @Test
    void createRequestRejectsNegativeAge() {
        CreateCustomerRequest request = new CreateCustomerRequest(
                "Test User",
                "M",
                -1,
                "ID-456",
                null,
                "Street 456",
                "555-222",
                "secret"
        );

        Set<ConstraintViolation<CreateCustomerRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
    }
}
