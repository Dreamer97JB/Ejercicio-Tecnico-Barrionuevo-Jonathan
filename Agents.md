# AGENTS.md — Reto Técnico Backend (Sofka) — MonoRepo Microservicios (Java 21, Spring Boot 3.5.9)

## Objetivo
Implementar un sistema bancario con 2 microservicios Spring Boot:
- customer-service: gestión de clientes (Persona/Cliente) y publicación de eventos asíncronos.
- account-service: gestión de cuentas, movimientos y reportes, consumiendo eventos para mantener un snapshot local del cliente.
Infra: PostgreSQL + RabbitMQ en docker-compose.
Requisito clave: comunicación asíncrona entre microservicios.

## Estructura real del repositorio
Root:
- services/customer-service
- services/account-service
- BaseDatos.sql
- docker-compose.yml
- postman_collection.json
- README.md

Paquetes existentes (no renombrar):
- customer-service: com.reto.tecnico.customer_service
- account-service: com.reto.tecnico.account_service

## Stack / Versiones
- Java 21
- Spring Boot 3.5.9
- Spring Web, Spring Data JPA, Validation, Lombok
- PostgreSQL
- RabbitMQ (spring-boot-starter-amqp)
- BCrypt: usar `spring-security-crypto` (NO incluir Spring Security Starter completo)

## Decisiones funcionales (NO asumir otras)
- clienteId: UUID (PK interna)
- identificacion: string libre UNIQUE (cédula/pasaporte/RUC/otro)
- tipoIdentificacion: string libre (NO catálogo ni tabla)
- No sobregiro: retiro que deje saldo < 0 => responder con mensaje EXACTO: "Saldo no disponible"
- Movimientos: request tiene movementType (DEPOSITO|RETIRO) y amount > 0. El backend calcula el balance.
- Eliminación: no borrar físico, usar soft delete via `active=false` en cliente/cuenta.

## Persistencia
### customer_db (customer-service)
Tabla: customers
Columnas (exactas):
- cliente_id (UUID PK)
- name (varchar)
- gender (varchar)
- age (int)
- identificacion (varchar unique)
- tipo_identificacion (varchar nullable)
- address (varchar)
- phone (varchar)
- password_hash (varchar)
- active (boolean)
- created_at (timestamptz)
- updated_at (timestamptz)

### account_db (account-service)
Tablas: client_snapshot, accounts, movements, processed_events
- client_snapshot: cliente_id (PK), identificacion (unique), tipo_identificacion, name, active, last_event_id, last_event_at, updated_at
- accounts: account_number (PK), account_type, initial_balance, current_balance, active, cliente_id (FK->client_snapshot.cliente_id), created_at, updated_at
- movements: movement_id (PK UUID), account_number (FK), movement_date, movement_type, amount, balance_after, created_at
- processed_events: event_id (PK UUID), processed_at

## Reglas de mapeo JPA
- No confiar en naming automático. Usar @Table y @Column(name="...") explícito para todas las columnas snake_case.
- IDs UUID: generar en el servicio con UUID.randomUUID() (simple y controlado).
- Config inicial: `spring.jpa.hibernate.ddl-auto=none` durante desarrollo. Al terminar: `validate`.

## RabbitMQ (Asíncrono)
- Exchange (topic): customer.events
- Routing keys: customer.created, customer.updated, customer.deactivated
- Queue en account-service: account.customer.events
- Binding: customer.*
- Evento JSON:
  - eventId (UUID)
  - eventType (CustomerCreated|CustomerUpdated|CustomerDeactivated)
  - occurredAt (ISO-8601)
  - payload:
    - clienteId (UUID)
    - identificacion (string)
    - tipoIdentificacion (string|null)
    - name (string)
    - active (boolean)

## Idempotencia y robustez del consumidor
- account-service debe registrar cada eventId en processed_events.
- Si el eventId ya existe: ignorar (no reprocesar).
- Upsert client_snapshot por cliente_id (si existe, update; si no, insert).
- Mantener last_event_id y last_event_at.

## API — customer-service (base URL: http://localhost:8081)
Endpoints (JSON):
- POST /clientes
- GET /clientes/{clienteId}
- GET /clientes?identificacion=...
- PUT /clientes/{clienteId}
- DELETE /clientes/{clienteId}  (soft delete: active=false)

Request/Response (DTOs):
- CreateCustomerRequest:
  - name, gender, age, identificacion, tipoIdentificacion (nullable), address, phone, password
- CustomerResponse:
  - clienteId, name, gender, age, identificacion, tipoIdentificacion, address, phone, active
- UpdateCustomerRequest: igual que create pero sin password obligatorio (si se envía password, re-hashear)
- Delete: solo marca active=false

BCrypt:
- password_hash = BCrypt hash del password recibido.
- Nunca exponer password_hash en respuestas.

Validaciones mínimas (Bean Validation):
- NotBlank en strings clave, age >= 0, amount > 0, etc.

## API — account-service (base URL: http://localhost:8082)
Endpoints:
- POST /cuentas
- GET /cuentas/{accountNumber}
- GET /cuentas?clienteId=... (opcional)
- PUT /cuentas/{accountNumber}
- DELETE /cuentas/{accountNumber} (soft delete)
- POST /movimientos
- GET /reportes?fechaDesde=YYYY-MM-DD&fechaHasta=YYYY-MM-DD&clienteId=...
- GET /reportes?fechaDesde=YYYY-MM-DD&fechaHasta=YYYY-MM-DD&identificacion=...

DTOs:
- CreateAccountRequest: accountNumber, accountType, initialBalance, clienteId
- AccountResponse: accountNumber, accountType, initialBalance, currentBalance, active, clienteId
- CreateMovementRequest: accountNumber, movementType, amount
- MovementResponse: movementId, accountNumber, movementType, amount, balanceAfter, movementDate
- ReportResponse: datos del cliente (clienteId, identificacion, name), lista de cuentas con movimientos en rango

Reglas:
- Crear cuenta: cliente debe existir en client_snapshot y active=true.
- Registrar movimiento: cuenta active=true. Aplicar depósito/retiro. Si retiro y saldo < 0 => error "Saldo no disponible".
- Concurrencia: para movimientos, bloquear la fila de Account con PESSIMISTIC_WRITE y @Transactional para evitar doble retiro simultáneo.

## Errores (ambos servicios)
Implementar @RestControllerAdvice común por servicio.
Formato JSON de error:
- timestamp
- status
- error
- message
- path

Códigos:
- 400: validación
- 404: no encontrado
- 409: conflicto (duplicado, cliente inactivo, saldo no disponible, etc.)
Mensaje EXACTO para saldo: "Saldo no disponible"

## Pruebas requeridas
- Unit test (customer-service): prueba de hashing / regla de active / validación simple de dominio.
- Integration test (account-service): registrar movimiento y verificar:
  - movimiento persistido
  - saldo actualizado
  - retiro con saldo insuficiente falla con 409 y mensaje exacto.
Recomendación: usar Testcontainers para Postgres + RabbitMQ si es viable, si no: usar @DataJpaTest + H2 para unit, pero integración ideal con Postgres.

## No alcance
- No implementar autenticación JWT ni roles.
- No implementar catálogos, ni migraciones Flyway/Liquibase (salvo que sea estrictamente necesario).
- Mantener código simple y coherente, priorizando completar requisitos.

## Entregables finales
- docker-compose up -d --build levanta todo.
- BaseDatos.sql incluido y funcional.
- Postman collection lista con variables:
  - customerBaseUrl=http://localhost:8081
  - accountBaseUrl=http://localhost:8082
- README con pasos de ejecución y ejemplos de requests.

## Updates
- customer-service: added Customer entity + repository with explicit snake_case mappings.
- customer-service: added DTOs, service, controller for /clientes CRUD + search by identificacion.
- customer-service: added BCrypt hashing + RabbitMQ exchange config and event publisher.
- customer-service: added RestControllerAdvice error handling format.
- customer-service: added unit tests for hashing/active and validation.
- account-service: added ClientSnapshot + ProcessedEvent entities/repositories.
- account-service: added RabbitMQ consumer and idempotent upsert for client_snapshot.
- account-service: added Account/Movement entities and repositories with explicit mappings.
- account-service: added /cuentas CRUD + /movimientos with transactional balance updates and PESSIMISTIC_WRITE lock.
- account-service: added RestControllerAdvice error format and conflict handling for saldo no disponible.
- account-service: added /reportes endpoint with clienteId/identificacion lookup and movements by date range.
- tests: added Testcontainers-based integration coverage for customer-service and account-service, plus schema.sql for test DB setup.
- errors: aligned account-service/customer-service advice with explicit EntityNotFoundException -> 404 mapping.
- time: added injectable Clock beans and used them for event occurrence and movement timestamps.
- rabbitmq: centralized exchange/queue/routing names via app.rabbit properties with JSON converter usage.
- customer-service: centralized password hashing in PasswordHasher and updated service/tests to avoid exposing hashes.
- account-service: added EventProcessingService for transactional idempotent event processing.
- openapi: added springdoc-openapi UI/docs config and controller tags for both services.
- errors: map missing routes/resources to 404 to avoid 500 on /actuator/mappings or swagger resources.
- customer-service: introduced Person mapped superclass and refactored Customer to extend it.
- account-service: added movement traceability (status/void/rectify), reconciliation logic, and CRUD endpoints for /movimientos.
- database: added additive ALTER TABLE patch for movements traceability columns in BaseDatos.sql.

## Test Audit (2026-01-22)

### How to run tests
- customer-service: `cd services/customer-service && ./mvnw test`
- account-service: `cd services/account-service && ./mvnw test`
- No root aggregator POM detected; run per service.

### Evidence (Surefire reports)
- customer-service: `services/customer-service/target/surefire-reports/com.reto.tecnico.customer_service.CustomerServiceApplicationTests.txt`
  - LastWriteTime: 22/01/2026 0:35:47
  - Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
- customer-service: `services/customer-service/target/surefire-reports/com.reto.tecnico.customer_service.CustomerServiceIntegrationTests.txt`
  - LastWriteTime: 22/01/2026 0:36:10
  - Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
- account-service: `services/account-service/target/surefire-reports/com.reto.tecnico.account_service.AccountServiceApplicationTests.txt`
  - LastWriteTime: 22/01/2026 1:03:52
  - Tests run: 9, Failures: 0, Errors: 0, Skipped: 0

### customer-service
- `services/customer-service/src/test/java/com/reto/tecnico/customer_service/CustomerServiceApplicationTests.java`
  - Type: UNIT (MockitoExtension, mocks, no Spring context/Testcontainers).
  - Dependencies: Mockito, PasswordHasher (BCrypt), Bean Validation Validator.
  - Covers:
    - `createHashesPasswordAndSetsActive`: hashes password, sets active, publishes event (mocked).
    - `createRequestRejectsNegativeAge`: validation rejects negative age.
  - Fixtures: CreateCustomerRequest with `ID-123`, `ID-456`, password `secret`.

- `services/customer-service/src/test/java/com/reto/tecnico/customer_service/CustomerServiceIntegrationTests.java`
  - Type: INTEGRATION (@SpringBootTest, @AutoConfigureMockMvc, @Testcontainers).
  - Dependencies: MockMvc, PostgreSQLContainer (postgres:16-alpine), RabbitMQContainer (rabbitmq:3.13-alpine),
    RabbitTemplate/AmqpAdmin, Awaitility, CustomerRepository.
  - Endpoints/routes:
    - POST `/clientes` with JSON body: name/gender/age/identificacion/tipoIdentificacion/address/phone/password.
  - Covers:
    - Create customer: 201, active=true, response hides password hash, DB hash matches.
    - Duplicate identificacion: 409.
    - Event published to exchange `customer.events` with routing `customer.created`; validates JSON payload.
  - Fixtures: identificacion `ID-100`, `ID-200`, `ID-300`; password `secret`.

### account-service
- `services/account-service/src/test/java/com/reto/tecnico/account_service/AccountServiceApplicationTests.java`
  - Type: INTEGRATION (@SpringBootTest, @AutoConfigureMockMvc, @Testcontainers).
  - Dependencies: MockMvc, PostgreSQLContainer (postgres:16-alpine), RabbitMQContainer (rabbitmq:3.13-alpine),
    RabbitTemplate, Awaitility, repositories (ClientSnapshot/Account/Movement/ProcessedEvent).
  - Endpoints/routes:
    - POST `/movimientos` (CreateMovementRequest).
    - GET `/movimientos` (query by accountNumber).
    - PUT `/movimientos/{movementId}` (UpdateMovementRequest).
    - DELETE `/movimientos/{movementId}` (VoidMovementRequest).
    - GET `/reportes` with `fechaDesde`, `fechaHasta`, `identificacion`.
  - Covers:
    - Customer event consumption updates `client_snapshot` and `processed_events`.
    - Idempotent event processing (event published twice).
    - POST /movimientos RETIRO insufficient funds -> 409 + message.
    - POST /movimientos DEPOSITO updates balance and persists movement.
    - GET /reportes filters movements by date range and identificacion.
    - GET /movimientos list returns ACTIVE movements and updated balances.
    - DELETE /movimientos voids deposit, creates reversal, reconciles balance.
    - PUT /movimientos rectifies withdrawal with reversal + replacement, reconciles balance.
    - DELETE /movimientos fails with 422 when reconciliation would go negative; DB unchanged.
  - Fixtures:
    - Snapshot IDs: `ID-300`..`ID-900`.
    - Accounts: `ACC-300`..`ACC-900`.
    - Movements: DEPOSITO/RETIRO with amounts 10/20/25/30/50/60.
    - Manual Movement seed for report date range (2026-01-15 vs 2026-02-01).

### Totals
- Test classes: 3
- Tests: 14
- Unit: 1 class / 2 tests
- Integration: 2 classes / 12 tests

### Reto requirements vs current status
- F5 (2 unit tests of endpoints, one must be Cliente): FAIL
  - Evidence: only unit test is `CustomerServiceApplicationTests` (service-level, Mockito), no @WebMvcTest or controller unit tests.
- F6 (1 integration test): PASS
  - Evidence: `CustomerServiceIntegrationTests` and `AccountServiceApplicationTests` are @SpringBootTest + Testcontainers.

## F5 Fix
- Added WebMvc unit tests (no DB/Testcontainers):
  - `services/customer-service/src/test/java/com/reto/tecnico/customer_service/controller/CustomerControllerWebMvcTest.java`
    - Endpoint: POST `/clientes`
    - Verifies: 201, response fields, service called with expected `CreateCustomerRequest`.
  - `services/account-service/src/test/java/com/reto/tecnico/account_service/controller/AccountControllerWebMvcTest.java`
    - Endpoint: POST `/cuentas`
    - Verifies: 201, response fields, service called with expected `CreateAccountRequest`.
- Run only these tests:
  - customer-service: `cd services/customer-service && ./mvnw -Dtest=CustomerControllerWebMvcTest test`
  - account-service: `cd services/account-service && ./mvnw -Dtest=AccountControllerWebMvcTest test`
- Status:
  - F5: PASS (2 endpoint unit tests, one for Cliente).
  - F6: PASS (integration tests exist), but full `mvn test` failed locally due to missing Docker for Testcontainers.

## Postman
- Import: in Postman, click Import and select `postman_collection.json` from repo root.
- Variables:
  - `baseCustomer` = `http://localhost:8081`
  - `baseAccount` = `http://localhost:8082`
- Requests included:
  - customer-service: POST/GET/PUT/DELETE `/clientes`, GET by identificacion.
  - account-service: POST/GET/PUT/DELETE `/cuentas`, POST `/movimientos`, GET `/reportes`.
