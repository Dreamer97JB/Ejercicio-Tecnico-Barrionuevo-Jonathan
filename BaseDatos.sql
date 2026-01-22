
-- =========================
-- 1) ROLES
-- =========================
DO $$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'customer_app') THEN
    CREATE ROLE customer_app LOGIN PASSWORD 'customer_pass';
  END IF;

  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'account_app') THEN
    CREATE ROLE account_app LOGIN PASSWORD 'account_pass';
  END IF;
END $$;

-- Timezone por defecto (Ecuador) para las sesiones de esos roles
ALTER ROLE customer_app SET timezone TO 'America/Guayaquil';
ALTER ROLE account_app  SET timezone TO 'America/Guayaquil';

-- =========================
-- 2) DATABASES
-- =========================
SELECT format('CREATE DATABASE %I OWNER %I', 'customer_db', 'customer_app')
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'customer_db')
\gexec

SELECT format('CREATE DATABASE %I OWNER %I', 'account_db', 'account_app')
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'account_db')
\gexec

-- Timezone por DB (también Ecuador)
ALTER DATABASE customer_db SET timezone TO 'America/Guayaquil';
ALTER DATABASE account_db  SET timezone TO 'America/Guayaquil';

-- =========================
-- 3) CUSTOMER_DB
-- =========================
\connect customer_db

REVOKE ALL ON SCHEMA public FROM PUBLIC;

-- Asegura owner del schema
ALTER SCHEMA public OWNER TO customer_app;

-- Extensión 
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Crear tabla (siempre)
CREATE TABLE IF NOT EXISTS public.customers (
  cliente_id          UUID PRIMARY KEY,
  name                VARCHAR(120) NOT NULL,
  gender              VARCHAR(20)  NOT NULL,
  age                 INT          NOT NULL CHECK (age >= 0),
  identificacion      VARCHAR(50)  NOT NULL UNIQUE,
  tipo_identificacion VARCHAR(30)  NULL,
  address             VARCHAR(255) NOT NULL,
  phone               VARCHAR(30)  NOT NULL,
  password_hash       VARCHAR(255) NOT NULL,
  active              BOOLEAN      NOT NULL DEFAULT TRUE,
  created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now()
);


ALTER TABLE public.customers OWNER TO customer_app;

-- Permisos explícitos
GRANT CONNECT ON DATABASE customer_db TO customer_app;
GRANT USAGE ON SCHEMA public TO customer_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE public.customers TO customer_app;


\connect customer_db customer_app
ALTER DEFAULT PRIVILEGES IN SCHEMA public
  GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO customer_app;


\connect customer_db

-- =========================
-- 4) ACCOUNT_DB
-- =========================
\connect account_db

REVOKE ALL ON SCHEMA public FROM PUBLIC;
ALTER SCHEMA public OWNER TO account_app;

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS public.client_snapshot (
  cliente_id          UUID PRIMARY KEY,
  identificacion      VARCHAR(50)  NOT NULL UNIQUE,
  tipo_identificacion VARCHAR(30)  NULL,
  name                VARCHAR(120) NOT NULL,
  active              BOOLEAN      NOT NULL,
  last_event_id       UUID         NULL,
  last_event_at       TIMESTAMPTZ  NULL,
  updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS public.accounts (
  account_number   VARCHAR(30) PRIMARY KEY,
  account_type     VARCHAR(30) NOT NULL,
  initial_balance  NUMERIC(19,2) NOT NULL CHECK (initial_balance >= 0),
  current_balance  NUMERIC(19,2) NOT NULL CHECK (current_balance >= 0),
  active           BOOLEAN NOT NULL DEFAULT TRUE,
  cliente_id       UUID NOT NULL,
  created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT fk_accounts_cliente
    FOREIGN KEY (cliente_id) REFERENCES public.client_snapshot(cliente_id)
);

CREATE TABLE IF NOT EXISTS public.movements (
  movement_id     UUID PRIMARY KEY,
  account_number  VARCHAR(30) NOT NULL,
  movement_date   TIMESTAMPTZ NOT NULL DEFAULT now(),
  movement_type   VARCHAR(20) NOT NULL, -- DEPOSITO | RETIRO
  amount          NUMERIC(19,2) NOT NULL CHECK (amount > 0),
  balance_after   NUMERIC(19,2) NOT NULL CHECK (balance_after >= 0),
  created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),

  status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE | VOIDED | SUPERSEDED
  voided_at       TIMESTAMPTZ NULL,
  void_reason     VARCHAR(255) NULL,
  reversal_movement_id UUID NULL,
  replacement_movement_id UUID NULL,

  CONSTRAINT fk_movements_account
    FOREIGN KEY (account_number) REFERENCES public.accounts(account_number)
);

CREATE TABLE IF NOT EXISTS public.processed_events (
  event_id       UUID PRIMARY KEY,
  processed_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Owners correctos
ALTER TABLE public.client_snapshot   OWNER TO account_app;
ALTER TABLE public.accounts          OWNER TO account_app;
ALTER TABLE public.movements         OWNER TO account_app;
ALTER TABLE public.processed_events  OWNER TO account_app;

-- Permisos
GRANT CONNECT ON DATABASE account_db TO account_app;
GRANT USAGE ON SCHEMA public TO account_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO account_app;

-- Default privileges para futuras tablas 
\connect account_db account_app
ALTER DEFAULT PRIVILEGES IN SCHEMA public
  GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO account_app;

-- Volvemos a postgres por seguridad
\connect account_db
