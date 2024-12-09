package com.webapp.bankingportal.exception;

public class AmountExceedLimitException extends RuntimeException {
    public AmountExceedLimitException(String message) {
        super(message);
    }
}