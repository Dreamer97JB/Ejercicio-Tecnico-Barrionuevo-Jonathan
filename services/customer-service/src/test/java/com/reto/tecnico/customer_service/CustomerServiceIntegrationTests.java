package com.reto.tecnico.customer_service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reto.tecnico.customer_service.config.RabbitProperties;
import com.reto.tecnico.customer_service.repository.CustomerRepository;
import com.reto.tecnico.customer_service.security.PasswordHasher;
import java.time.Duration;
import java.util.UUID;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
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
class CustomerServiceIntegrationTests {

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
    private CustomerRepository customerRepository;

    @Autowired
    private PasswordHasher passwordHasher;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private AmqpAdmin amqpAdmin;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RabbitProperties rabbitProperties;

    @BeforeEach
    void setUp() {
        customerRepository.deleteAll();
    }

    @Test
    void createCustomerHashesPasswordAndHidesHash() throws Exception {
        String payload = customerPayload("ID-100", "secret");

        MvcResult result = mockMvc.perform(post("/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clienteId").exists())
                .andExpect(jsonPath("$.active").value(true))
                .andReturn();

        String response = result.getResponse().getContentAsString();
        assertThat(response).doesNotContain("password_hash");
        assertThat(response).doesNotContain("passwordHash");

        String storedHash = customerRepository.findByIdentificacion("ID-100")
                .orElseThrow()
                .getPasswordHash();
        assertThat(passwordHasher.matches("secret", storedHash)).isTrue();
    }

    @Test
    void duplicateIdentificacionReturns409() throws Exception {
        String payload = customerPayload("ID-200", "secret");

        mockMvc.perform(post("/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict());
    }

    @Test
    void createCustomerPublishesEvent() throws Exception {
        String queueName = "test.customer.events." + UUID.randomUUID();
        Queue queue = new Queue(queueName, false, false, true);
        amqpAdmin.declareQueue(queue);

        TopicExchange exchange = new TopicExchange(rabbitProperties.getExchange());
        amqpAdmin.declareExchange(exchange);

        Binding binding = BindingBuilder.bind(queue)
                .to(exchange)
                .with(rabbitProperties.getRouting().getCreated());
        amqpAdmin.declareBinding(binding);

        String payload = customerPayload("ID-300", "secret");

        mockMvc.perform(post("/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());

        Awaitility.await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            Message message = rabbitTemplate.receive(queueName);
            assertThat(message).isNotNull();
            JsonNode node = objectMapper.readTree(message.getBody());
            assertThat(node.get("eventId").asText()).isNotBlank();
            assertThat(node.get("eventType").asText()).isEqualTo("CustomerCreated");
            assertThat(node.get("payload").get("identificacion").asText()).isEqualTo("ID-300");
            assertThat(node.get("payload").get("active").asBoolean()).isTrue();
        });
    }

    private String customerPayload(String identificacion, String password) {
        return """
                {
                  "name": "Test User",
                  "gender": "M",
                  "age": 30,
                  "identificacion": "%s",
                  "tipoIdentificacion": "CC",
                  "address": "Street 123",
                  "phone": "555-111",
                  "password": "%s"
                }
                """.formatted(identificacion, password);
    }
}
