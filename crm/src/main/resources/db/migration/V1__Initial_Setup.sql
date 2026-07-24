-- V1__Initial_Setup.sql (CRM)

CREATE TABLE propostas (
    id VARCHAR(36) PRIMARY KEY,
    cliente_nome VARCHAR(255) NOT NULL,
    cliente_email VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    version BIGINT DEFAULT 0
);

CREATE TABLE itens_proposta (
    id VARCHAR(36) PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    quantidade INT NOT NULL,
    preco_unitario DECIMAL(10,2) NOT NULL,
    proposta_id VARCHAR(36) NOT NULL,
    CONSTRAINT fk_item_proposta FOREIGN KEY (proposta_id) REFERENCES propostas(id) ON DELETE CASCADE
);

CREATE TABLE outbox_events (
    id VARCHAR(36) PRIMARY KEY,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);