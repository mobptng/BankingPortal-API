package com.webapp.bankingportal.exception;

/**
 * Exception thrown when a requested loan cannot be found.
 */
public class LoanNotFoundException extends RuntimeException {
    
    /**
     * Constructs a new LoanNotFoundException with the specified message.
     *
     * @param message the detail message
     */
    public LoanNotFoundException(String message) {
        super(message);
    }
}