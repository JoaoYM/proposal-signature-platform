package com.powermobile.crm.domain.exception;

public class PropostaDomainException extends RuntimeException {

    public PropostaDomainException(String message) {
        super(message);
    }

    public PropostaDomainException(String message, Throwable cause) {
        super(message, cause);
    }
}