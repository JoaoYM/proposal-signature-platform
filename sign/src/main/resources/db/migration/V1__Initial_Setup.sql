-- V1__Initial_Setup.sql (SIGN)

CREATE TABLE contratos (
    id VARCHAR(36) PRIMARY KEY,
    proposta_id VARCHAR(36) NOT NULL UNIQUE,
    conteudo TEXT NOT NULL,
    status VARCHAR(50) NOT NULL,
    version BIGINT DEFAULT 0
);

CREATE TABLE participantes (
    id VARCHAR(36) PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    ordem INT NOT NULL,
    data_hora DATETIME(6) NULL,
    ip_origem VARCHAR(255) NULL,
    hash_validacao VARCHAR(64) NULL,
    contrato_id VARCHAR(36) NOT NULL,
    CONSTRAINT fk_participante_contrato FOREIGN KEY (contrato_id) REFERENCES contratos(id) ON DELETE CASCADE
);

CREATE TABLE contrato_audit_log (
    id VARCHAR(36) PRIMARY KEY,
    contrato_id VARCHAR(36) NOT NULL,
    acao VARCHAR(100) NOT NULL,
    ator VARCHAR(255) NOT NULL,
    detalhes TEXT,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);

CREATE TABLE sign_outbox_events (
    id VARCHAR(36) PRIMARY KEY,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);

CREATE TABLE inbox_events (event_id VARCHAR(36) PRIMARY KEY, event_type VARCHAR(100) NOT NULL, aggregate_id VARCHAR(100) NOT NULL, occurred_at DATETIME(6) NOT NULL, processed_at DATETIME(6) NOT NULL);
