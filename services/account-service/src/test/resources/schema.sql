CREATE TABLE IF NOT EXISTS client_snapshot (
  cliente_id          UUID PRIMARY KEY,
  identificacion      VARCHAR(50)  NOT NULL UNIQUE,
  tipo_identificacion VARCHAR(30)  NULL,
  name                VARCHAR(120) NOT NULL,
  active              BOOLEAN      NOT NULL,
  last_event_id       UUID         NULL,
  last_event_at       TIMESTAMPTZ  NULL,
  updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now()
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
  movement_type   VARCHAR(20) NOT NULL,
  amount          NUMERIC(19,2) NOT NULL CHECK (amount > 0),
  balance_after   NUMERIC(19,2) NOT NULL CHECK (balance_after >= 0),
  created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT fk_movements_account
    FOREIGN KEY (account_number) REFERENCES accounts(account_number)
);

CREATE TABLE IF NOT EXISTS processed_events (
  event_id       UUID PRIMARY KEY,
  processed_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
