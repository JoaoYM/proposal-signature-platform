package com.powermobile.sign.domain.exception;

public class AssinaturaDomainException extends RuntimeException {

    public AssinaturaDomainException(String message) {
        super(message);
    }

    public AssinaturaDomainException(String message, Throwable cause) {
        super(message, cause);
    }
}