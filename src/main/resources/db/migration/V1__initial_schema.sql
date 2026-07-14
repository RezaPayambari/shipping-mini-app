CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE orders (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    external_order_number VARCHAR(255) NOT NULL UNIQUE,
    street                VARCHAR(255) NOT NULL,
    zip_code              VARCHAR(255) NOT NULL,
    city                  VARCHAR(255) NOT NULL,
    country               VARCHAR(255) NOT NULL,
    status                VARCHAR(50)  NOT NULL DEFAULT 'CREATED',
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE order_positions (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id    UUID         NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    sku         VARCHAR(255) NOT NULL,
    quantity    INTEGER      NOT NULL,
    description VARCHAR(255)
);

CREATE TABLE shipments (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id   UUID        NOT NULL UNIQUE REFERENCES orders(id) ON DELETE CASCADE,
    status     VARCHAR(50) NOT NULL DEFAULT 'CREATED',
    shipped_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE packages (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    shipment_id   UUID         NOT NULL REFERENCES shipments(id) ON DELETE CASCADE,
    tracking_code VARCHAR(255) UNIQUE,
    carrier       VARCHAR(255)
);

CREATE INDEX idx_order_positions_order_id ON order_positions(order_id);
CREATE INDEX idx_packages_shipment_id     ON packages(shipment_id);
