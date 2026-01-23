CREATE TABLE IF NOT EXISTS customers (
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
