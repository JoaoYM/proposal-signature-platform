package com.powermobile.crm.domain.exception;

import java.util.UUID;

public class PropostaNotFoundException extends RuntimeException {

    public PropostaNotFoundException(UUID id) {
        super("Proposta não encontrada: " + id);
    }

    public PropostaNotFoundException(String message) {
        super(message);
    }
}