package com.powermobile.sign.domain.exception;

import java.util.UUID;

public class ContratoNotFoundException extends RuntimeException {

    public ContratoNotFoundException(UUID id) {
        super("Contrato não encontrado: " + id);
    }

    public ContratoNotFoundException(String message) {
        super(message);
    }
}