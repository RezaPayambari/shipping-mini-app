-- Optionales Referenz-Skript.
-- Wird NICHT benötigt, solange application.yml "ddl-auto: update" gesetzt hat --
-- dann legt Hibernate/JPA dieses Schema automatisch beim App-Start an.
-- Nützlich nur, falls ihr manuell prüfen, das Schema vorab anlegen,
-- oder später auf ein Migrationstool (Flyway/Liquibase) umsteigen wollt.

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE IF NOT EXISTS orders (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    external_order_number VARCHAR(255) NOT NULL UNIQUE,
    street                VARCHAR(255),
    zip_code              VARCHAR(255),
    city                  VARCHAR(255),
    country               VARCHAR(255),
    status                VARCHAR(50) NOT NULL DEFAULT 'CREATED',
    created_at            TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS order_positions (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id    UUID NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    sku         VARCHAR(255) NOT NULL,
    quantity    INTEGER NOT NULL,
    description VARCHAR(255)x
);

CREATE TABLE IF NOT EXISTS shipments (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id    UUID NOT NULL UNIQUE REFERENCES orders (id) ON DELETE CASCADE,
    status      VARCHAR(50) NOT NULL DEFAULT 'CREATED',
    shipped_at  TIMESTAMP,
    created_at  TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS packages (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    shipment_id   UUID NOT NULL REFERENCES shipments (id) ON DELETE CASCADE,
    tracking_code VARCHAR(255) UNIQUE,
    carrier       VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_order_positions_order_id ON order_positions (order_id);
CREATE INDEX IF NOT EXISTS idx_packages_shipment_id ON packages (shipment_id);
