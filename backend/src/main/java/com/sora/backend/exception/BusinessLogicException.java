package com.sora.backend.exception;

import org.springframework.http.HttpStatus;

public class BusinessLogicException extends RuntimeException {
    
    private final String code;
    private final String details;
    private final HttpStatus status;

    public BusinessLogicException(String code, String message, String details, HttpStatus status) {
        super(message);
        this.code = code;
        this.details = details;
        this.status = status;
    }

    public BusinessLogicException(String code, String message, HttpStatus status) {
        this(code, message, null, status);
    }

    public String getCode() {
        return code;
    }

    public String getDetails() {
        return details;
    }

    public HttpStatus getStatus() {
        return status;
    }
}