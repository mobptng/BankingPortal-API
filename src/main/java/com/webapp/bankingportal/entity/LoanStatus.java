package com.webapp.bankingportal.entity;

/**
 * Enum representing the possible states of a loan in the banking portal.
 */
public enum LoanStatus {
    /**
     * Represents a loan that is pending approval
     */
    PENDING,
    
    /**
     * Represents a loan that has been approved
     */
    APPROVED,
    
    /**
     * Represents a loan that has been fully repaid
     */
    REPAID
}