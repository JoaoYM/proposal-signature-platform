package com.powermobile.crm.api.exception;

import java.util.List;

public record ErrorResponse(String message, String errorCode, List<String> details) {
    public ErrorResponse(String message, String errorCode) {
        this(message, errorCode, List.of());
    }
}