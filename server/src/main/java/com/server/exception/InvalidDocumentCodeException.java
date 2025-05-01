package com.server.exception;

public class InvalidDocumentCodeException extends RuntimeException {
    public InvalidDocumentCodeException(String message) {
        super(message);
    }
}