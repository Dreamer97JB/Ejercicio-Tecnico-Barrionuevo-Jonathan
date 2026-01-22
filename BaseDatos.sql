
-- 1) Roles 
DO $$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'customer_app') THEN
    CREATE ROLE customer_app LOGIN PASSWORD 't3$t1Ng972g';
  END IF;
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'account_app') THEN
    CREATE ROLE account_app  LOGIN PASSWORD 't3$t1Ng972g';
  END IF;
END $$;

-- 2) Databases
DO $$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_database WHERE datname = 'customer_db') THEN
    CREATE DATABASE customer_db OWNER customer_app;
  END IF;
  IF NOT EXISTS (SELECT FROM pg_database WHERE datname = 'account_db') THEN
    CREATE DATABASE account_db OWNER account_app;
  END IF;
END $$;

-- -------------------------
-- CUSTOMER_DB
-- -------------------------
\connect customer_db

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS customers (
  cliente_id      UUID PRIMARY KEY,
  name            VARCHAR(120) NOT NULL,
  gender          VARCHAR(20)  NOT NULL,
  age             INT          NOT NULL CHECK (age >= 0),
  identificacion  VARCHAR(20)  NOT NULL UNIQUE,
  address         VARCHAR(255) NOT NULL,
  phone           VARCHAR(30)  NOT NULL,
  password_hash   VARCHAR(255) NOT NULL,
  active          BOOLEAN      NOT NULL DEFAULT TRUE,
  created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- -------------------------
-- ACCOUNT_DB
-- -------------------------
\connect account_db

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS client_snapshot (
  cliente_id      UUID PRIMARY KEY,
  identificacion  VARCHAR(20)  NOT NULL UNIQUE,
  name            VARCHAR(120) NOT NULL,
  active          BOOLEAN      NOT NULL,
  last_event_id   UUID         NULL,
  last_event_at   TIMESTAMPTZ  NULL,
  updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS accounts (
  account_number   VARCHAR(30) PRIMARY KEY,
  account_type     VARCHAR(30) NOT NULL,
  initial_balance  NUMERIC(19,2) NOT NULL CHECK (initial_balance >= 0),
  current_balance  NUMERIC(19,2) NOT NULL CHECK (current_balance >= 0),
  active           BOOLEAN NOT NULL DEFAULT TRUE,
  cliente_id       UUID NOT NULL,
  created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT fk_accounts_cliente
    FOREIGN KEY (cliente_id) REFERENCES client_snapshot(cliente_id)
);

CREATE TABLE IF NOT EXISTS movements (
  movement_id     UUID PRIMARY KEY,
  account_number  VARCHAR(30) NOT NULL,
  movement_date   TIMESTAMPTZ NOT NULL DEFAULT now(),
  movement_type   VARCHAR(20) NOT NULL, -- DEPOSITO | RETIRO
  amount          NUMERIC(19,2) NOT NULL CHECK (amount > 0),
  balance_after   NUMERIC(19,2) NOT NULL CHECK (balance_after >= 0),
  created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT fk_movements_account
    FOREIGN KEY (account_number) REFERENCES accounts(account_number)
);

-- Idempotencia de eventos procesados
CREATE TABLE IF NOT EXISTS processed_events (
  event_id       UUID PRIMARY KEY,
  processed_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
