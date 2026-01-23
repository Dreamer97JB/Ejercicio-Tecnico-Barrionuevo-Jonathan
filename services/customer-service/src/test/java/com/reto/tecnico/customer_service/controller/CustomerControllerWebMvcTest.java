package com.reto.tecnico.customer_service.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reto.tecnico.customer_service.dto.CreateCustomerRequest;
import com.reto.tecnico.customer_service.dto.CustomerResponse;
import com.reto.tecnico.customer_service.service.CustomerService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CustomerController.class)
class CustomerControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CustomerService customerService;

    @Test
    void createReturnsCreatedAndCallsService() throws Exception {
        CreateCustomerRequest request = new CreateCustomerRequest(
                "Test User",
                "M",
                30,
                "ID-100",
                "CC",
                "Street 123",
                "555-111",
                "secret"
        );
        CustomerResponse response = new CustomerResponse(
                UUID.randomUUID(),
                request.name(),
                request.gender(),
                request.age(),
                request.identificacion(),
                request.tipoIdentificacion(),
                request.address(),
                request.phone(),
                true
        );

        when(customerService.create(request)).thenReturn(response);

        mockMvc.perform(post("/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clienteId").value(response.clienteId().toString()))
                .andExpect(jsonPath("$.identificacion").value(request.identificacion()))
                .andExpect(jsonPath("$.active").value(true));

        ArgumentCaptor<CreateCustomerRequest> captor = ArgumentCaptor.forClass(CreateCustomerRequest.class);
        verify(customerService).create(captor.capture());
        assertThat(captor.getValue()).isEqualTo(request);
    }
}
