package com.reto.tecnico.account_service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reto.tecnico.account_service.config.RabbitProperties;
import com.reto.tecnico.account_service.dto.CreateMovementRequest;
import com.reto.tecnico.account_service.dto.UpdateMovementRequest;
import com.reto.tecnico.account_service.dto.VoidMovementRequest;
import com.reto.tecnico.account_service.entity.Account;
import com.reto.tecnico.account_service.entity.ClientSnapshot;
import com.reto.tecnico.account_service.entity.Movement;
import com.reto.tecnico.account_service.entity.MovementStatus;
import com.reto.tecnico.account_service.entity.MovementType;
import com.reto.tecnico.account_service.messaging.CustomerEvent;
import com.reto.tecnico.account_service.messaging.CustomerEventPayload;
import com.reto.tecnico.account_service.repository.AccountRepository;
import com.reto.tecnico.account_service.repository.ClientSnapshotRepository;
import com.reto.tecnico.account_service.repository.MovementRepository;
import com.reto.tecnico.account_service.repository.ProcessedEventRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AccountServiceApplicationTests {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    static final RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3.13-alpine");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.rabbitmq.host", rabbit::getHost);
        registry.add("spring.rabbitmq.port", rabbit::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbit::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbit::getAdminPassword);
        registry.add("spring.sql.init.mode", () -> "always");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RabbitProperties rabbitProperties;

    @Autowired
    private ClientSnapshotRepository clientSnapshotRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private MovementRepository movementRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @BeforeEach
    void cleanDatabase() {
        movementRepository.deleteAll();
        accountRepository.deleteAll();
        processedEventRepository.deleteAll();
        clientSnapshotRepository.deleteAll();
    }

    @Test
    void customerCreatedEventUpdatesSnapshotAndProcessedEvents() {
        UUID clienteId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        CustomerEvent event = new CustomerEvent(
                eventId,
                "CustomerCreated",
                OffsetDateTime.now(ZoneOffset.UTC),
                new CustomerEventPayload(clienteId, "ID-100", "CC", "Test Client", true)
        );

        rabbitTemplate.convertAndSend(rabbitProperties.getExchange(), rabbitProperties.getRouting().getCreated(), event);

        Awaitility.await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            ClientSnapshot snapshot = clientSnapshotRepository.findById(clienteId).orElse(null);
            assertThat(snapshot).isNotNull();
            assertThat(snapshot.getIdentificacion()).isEqualTo("ID-100");
            assertThat(snapshot.getLastEventId()).isEqualTo(eventId);
            assertThat(processedEventRepository.existsById(eventId)).isTrue();
        });
    }

    @Test
    void customerEventIsIdempotent() {
        UUID clienteId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        CustomerEvent event = new CustomerEvent(
                eventId,
                "CustomerCreated",
                OffsetDateTime.now(ZoneOffset.UTC),
                new CustomerEventPayload(clienteId, "ID-200", "CC", "Test Client", true)
        );

        rabbitTemplate.convertAndSend(rabbitProperties.getExchange(), rabbitProperties.getRouting().getCreated(), event);

        Awaitility.await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                assertThat(processedEventRepository.count()).isEqualTo(1)
        );

        rabbitTemplate.convertAndSend(rabbitProperties.getExchange(), rabbitProperties.getRouting().getCreated(), event);

        Awaitility.await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            assertThat(processedEventRepository.count()).isEqualTo(1);
            ClientSnapshot snapshot = clientSnapshotRepository.findById(clienteId).orElse(null);
            assertThat(snapshot).isNotNull();
            assertThat(snapshot.getLastEventId()).isEqualTo(eventId);
        });
    }

    @Test
    void retiroInsufficientFundsReturns409AndBalanceUnchanged() throws Exception {
        UUID clienteId = UUID.randomUUID();
        createSnapshot(clienteId, "ID-300");
        createAccount(clienteId, "ACC-300", new BigDecimal("50.00"));

        CreateMovementRequest request = new CreateMovementRequest(
                "ACC-300",
                MovementType.RETIRO,
                new BigDecimal("60.00")
        );

        mockMvc.perform(post("/movimientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Saldo no disponible"));

        Account account = accountRepository.findById("ACC-300").orElseThrow();
        assertThat(account.getCurrentBalance()).isEqualByComparingTo("50.00");
        assertThat(movementRepository.count()).isEqualTo(0);
    }

    @Test
    void depositoUpdatesBalanceAndMovement() throws Exception {
        UUID clienteId = UUID.randomUUID();
        createSnapshot(clienteId, "ID-400");
        createAccount(clienteId, "ACC-400", new BigDecimal("100.00"));

        CreateMovementRequest request = new CreateMovementRequest(
                "ACC-400",
                MovementType.DEPOSITO,
                new BigDecimal("25.00")
        );

        MvcResult result = mockMvc.perform(post("/movimientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        UUID movementId = UUID.fromString(response.get("movementId").asText());
        assertThat(response.get("balanceAfter").decimalValue())
                .isEqualByComparingTo(new BigDecimal("125.00"));

        Account account = accountRepository.findById("ACC-400").orElseThrow();
        assertThat(account.getCurrentBalance()).isEqualByComparingTo("125.00");

        Movement movement = movementRepository.findById(movementId).orElseThrow();
        assertThat(movement.getBalanceAfter()).isEqualByComparingTo("125.00");
    }

    @Test
    void reportFiltersMovementsByRangeAndIdentificacion() throws Exception {
        UUID clienteId = UUID.randomUUID();
        createSnapshot(clienteId, "ID-500");
        createAccount(clienteId, "ACC-500", new BigDecimal("100.00"));

        Movement inRange = new Movement();
        inRange.setMovementId(UUID.randomUUID());
        inRange.setAccountNumber("ACC-500");
        inRange.setMovementType(MovementType.DEPOSITO);
        inRange.setAmount(new BigDecimal("10.00"));
        inRange.setBalanceAfter(new BigDecimal("110.00"));
        inRange.setMovementDate(OffsetDateTime.of(2026, 1, 15, 10, 0, 0, 0, ZoneOffset.UTC));
        inRange.setCreatedAt(OffsetDateTime.of(2026, 1, 15, 10, 0, 0, 0, ZoneOffset.UTC));
        movementRepository.save(inRange);

        Movement outOfRange = new Movement();
        outOfRange.setMovementId(UUID.randomUUID());
        outOfRange.setAccountNumber("ACC-500");
        outOfRange.setMovementType(MovementType.DEPOSITO);
        outOfRange.setAmount(new BigDecimal("20.00"));
        outOfRange.setBalanceAfter(new BigDecimal("130.00"));
        outOfRange.setMovementDate(OffsetDateTime.of(2026, 2, 1, 10, 0, 0, 0, ZoneOffset.UTC));
        outOfRange.setCreatedAt(OffsetDateTime.of(2026, 2, 1, 10, 0, 0, 0, ZoneOffset.UTC));
        movementRepository.save(outOfRange);

        mockMvc.perform(get("/reportes")
                        .param("fechaDesde", "2026-01-01")
                        .param("fechaHasta", "2026-01-31")
                        .param("identificacion", "ID-500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identificacion").value("ID-500"))
                .andExpect(jsonPath("$.accounts.length()").value(1))
                .andExpect(jsonPath("$.accounts[0].movements.length()").value(1));
    }

    @Test
    void listMovementsAndBalances() throws Exception {
        UUID clienteId = UUID.randomUUID();
        createSnapshot(clienteId, "ID-600");
        createAccount(clienteId, "ACC-600", new BigDecimal("100.00"));

        createMovement("ACC-600", MovementType.DEPOSITO, new BigDecimal("50.00"));
        createMovement("ACC-600", MovementType.RETIRO, new BigDecimal("30.00"));

        Account account = accountRepository.findById("ACC-600").orElseThrow();
        assertThat(account.getCurrentBalance()).isEqualByComparingTo("120.00");

        mockMvc.perform(get("/movimientos")
                        .param("accountNumber", "ACC-600"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }

    @Test
    void voidDepositCreatesReversalAndReconcilesBalances() throws Exception {
        UUID clienteId = UUID.randomUUID();
        createSnapshot(clienteId, "ID-700");
        createAccount(clienteId, "ACC-700", new BigDecimal("0.00"));

        UUID depositId = createMovement("ACC-700", MovementType.DEPOSITO, new BigDecimal("50.00"));

        VoidMovementRequest request = new VoidMovementRequest("Customer request");
        MvcResult result = mockMvc.perform(delete("/movimientos/{movementId}", depositId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        UUID reversalId = UUID.fromString(response.get("reversalMovementId").asText());

        Movement original = movementRepository.findById(depositId).orElseThrow();
        assertThat(original.getStatus()).isEqualTo(MovementStatus.VOIDED);
        assertThat(original.getReversalMovementId()).isEqualTo(reversalId);

        Movement reversal = movementRepository.findById(reversalId).orElseThrow();
        assertThat(reversal.getStatus()).isEqualTo(MovementStatus.ACTIVE);
        assertThat(reversal.getMovementType()).isEqualTo(MovementType.RETIRO);

        Account account = accountRepository.findById("ACC-700").orElseThrow();
        assertThat(account.getCurrentBalance()).isEqualByComparingTo("0.00");
    }

    @Test
    void rectifyWithdrawalCreatesReversalAndReplacement() throws Exception {
        UUID clienteId = UUID.randomUUID();
        createSnapshot(clienteId, "ID-800");
        createAccount(clienteId, "ACC-800", new BigDecimal("100.00"));

        UUID withdrawalId = createMovement("ACC-800", MovementType.RETIRO, new BigDecimal("30.00"));

        UpdateMovementRequest request = new UpdateMovementRequest(
                MovementType.RETIRO,
                new BigDecimal("50.00"),
                null
        );

        MvcResult result = mockMvc.perform(put("/movimientos/{movementId}", withdrawalId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        UUID reversalId = UUID.fromString(response.get("reversalMovementId").asText());
        UUID replacementId = UUID.fromString(response.get("replacementMovementId").asText());

        Movement original = movementRepository.findById(withdrawalId).orElseThrow();
        assertThat(original.getStatus()).isEqualTo(MovementStatus.SUPERSEDED);
        assertThat(original.getReplacementMovementId()).isEqualTo(replacementId);

        Movement reversal = movementRepository.findById(reversalId).orElseThrow();
        assertThat(reversal.getStatus()).isEqualTo(MovementStatus.ACTIVE);
        assertThat(reversal.getMovementType()).isEqualTo(MovementType.DEPOSITO);

        Movement replacement = movementRepository.findById(replacementId).orElseThrow();
        assertThat(replacement.getStatus()).isEqualTo(MovementStatus.ACTIVE);
        assertThat(replacement.getAmount()).isEqualByComparingTo("50.00");

        Account account = accountRepository.findById("ACC-800").orElseThrow();
        assertThat(account.getCurrentBalance()).isEqualByComparingTo("50.00");
    }

    @Test
    void voidingDepositThatBreaksBalanceReturns422AndRollsBack() throws Exception {
        UUID clienteId = UUID.randomUUID();
        createSnapshot(clienteId, "ID-900");
        createAccount(clienteId, "ACC-900", new BigDecimal("0.00"));

        UUID depositId = createMovement("ACC-900", MovementType.DEPOSITO, new BigDecimal("50.00"));
        createMovement("ACC-900", MovementType.RETIRO, new BigDecimal("30.00"));

        mockMvc.perform(delete("/movimientos/{movementId}", depositId))
                .andExpect(status().isUnprocessableEntity());

        Movement original = movementRepository.findById(depositId).orElseThrow();
        assertThat(original.getStatus()).isEqualTo(MovementStatus.ACTIVE);
        assertThat(original.getReversalMovementId()).isNull();

        Account account = accountRepository.findById("ACC-900").orElseThrow();
        assertThat(account.getCurrentBalance()).isEqualByComparingTo("20.00");
        assertThat(movementRepository.count()).isEqualTo(2);
    }

    private ClientSnapshot createSnapshot(UUID clienteId, String identificacion) {
        ClientSnapshot snapshot = new ClientSnapshot();
        snapshot.setClienteId(clienteId);
        snapshot.setIdentificacion(identificacion);
        snapshot.setTipoIdentificacion("CC");
        snapshot.setName("Test Client");
        snapshot.setActive(true);
        return clientSnapshotRepository.save(snapshot);
    }

    private Account createAccount(UUID clienteId, String accountNumber, BigDecimal balance) {
        Account account = new Account();
        account.setAccountNumber(accountNumber);
        account.setAccountType("AHORROS");
        account.setInitialBalance(balance);
        account.setCurrentBalance(balance);
        account.setActive(true);
        account.setClienteId(clienteId);
        return accountRepository.save(account);
    }

    private UUID createMovement(String accountNumber, MovementType type, BigDecimal amount) throws Exception {
        CreateMovementRequest request = new CreateMovementRequest(accountNumber, type, amount);
        MvcResult result = mockMvc.perform(post("/movimientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(response.get("movementId").asText());
    }
}
