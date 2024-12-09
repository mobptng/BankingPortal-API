package com.webapp.bankingportal.exception;

public class LoanApprovalNotAllowedException extends RuntimeException {
    
    public LoanApprovalNotAllowedException(String message) {
        super(message);
    }
}