package com.webapp.bankingportal.exception;

public class LoanAlreadyApprovedException extends RuntimeException {
    public LoanAlreadyApprovedException(String message) {
        super(message);
    }
}