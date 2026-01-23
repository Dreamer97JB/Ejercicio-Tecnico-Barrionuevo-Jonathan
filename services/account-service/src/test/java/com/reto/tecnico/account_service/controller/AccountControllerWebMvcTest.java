package com.reto.tecnico.account_service.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reto.tecnico.account_service.dto.AccountResponse;
import com.reto.tecnico.account_service.dto.CreateAccountRequest;
import com.reto.tecnico.account_service.service.AccountService;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AccountController.class)
class AccountControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AccountService accountService;

    @Test
    void createReturnsCreatedAndCallsService() throws Exception {
        UUID clienteId = UUID.randomUUID();
        CreateAccountRequest request = new CreateAccountRequest(
                "ACC-1001",
                "AHORROS",
                new BigDecimal("100.00"),
                clienteId
        );
        AccountResponse response = new AccountResponse(
                request.accountNumber(),
                request.accountType(),
                request.initialBalance(),
                request.initialBalance(),
                true,
                request.clienteId()
        );

        when(accountService.create(request)).thenReturn(response);

        mockMvc.perform(post("/cuentas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountNumber").value(request.accountNumber()))
                .andExpect(jsonPath("$.clienteId").value(clienteId.toString()))
                .andExpect(jsonPath("$.active").value(true));

        ArgumentCaptor<CreateAccountRequest> captor = ArgumentCaptor.forClass(CreateAccountRequest.class);
        verify(accountService).create(captor.capture());
        assertThat(captor.getValue()).isEqualTo(request);
    }
}
