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
