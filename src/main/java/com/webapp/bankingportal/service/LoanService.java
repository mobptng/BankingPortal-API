
package com.webapp.bankingportal.service;

import com.webapp.bankingportal.entity.Loan;
import com.webapp.bankingportal.exception.AccountDoesNotExistException;
import com.webapp.bankingportal.exception.InvalidAmountException;
import com.webapp.bankingportal.exception.LoanNotFoundException;
import java.util.List;

import com.webapp.bankingportal.entity.Loan;
import com.webapp.bankingportal.exception.AccountDoesNotExistException;
import com.webapp.bankingportal.exception.InvalidAmountException;
import com.webapp.bankingportal.exception.LoanNotFoundException;
import com.webapp.bankingportal.exception.NotImplementedException;

import java.util.List;

/**
 * Service interface for managing loan operations.
 */
public interface LoanService {

    /**
     * Apply for a new loan.
     *
     * @param accountNumber Account number of the loan applicant
     * @param amount Amount of loan requested
     * @param description Purpose or description of the loan
     * @return The created loan object
     * @throws AccountDoesNotExistException if the account does not exist
     * @throws InvalidAmountException if the loan amount is invalid
     */
    /**
     * Apply for a new loan.
     *
     * @param accountNumber Account number of the loan applicant
     * @param amount Amount of loan requested (must be positive and not exceed twice the account balance)
     * @param description Purpose or description of the loan
     * @return The created loan object with PENDING status
     * @throws AccountDoesNotExistException if the account does not exist
     * @throws InvalidAmountException if the amount is negative or exceeds the maximum allowed
     */
    Loan applyForLoan(String accountNumber, double amount, String description);

    /**
     * Approve a loan application.
     *
     * @param loanId ID of the loan to be approved
     * @return The approved loan object
     * @throws LoanNotFoundException if the loan is not found
     */
    /**
     * Approve a loan application based on predefined criteria.
     * The loan amount should not exceed twice the account balance.
     *
     * @param loanId ID of the loan to be approved
     * @return The approved loan object with updated status
     * @throws LoanNotFoundException if the loan is not found
     * @throws InvalidAmountException if the loan amount exceeds the allowed limit
     */
    Loan approveLoan(Long loanId);

    /**
     * Make a repayment towards a loan.
     *
     * @param loanId ID of the loan
     * @param amount Amount to be repaid
     * @throws LoanNotFoundException if the loan is not found
     * @throws InvalidAmountException if the repayment amount is invalid
     */
    /**
     * Process a loan repayment.
     *
     * @param loanId ID of the loan
     * @param amount Amount to be repaid
     * @throws LoanNotFoundException if the loan is not found
     * @throws InvalidAmountException if the amount is negative or exceeds the outstanding balance
     */
    void repayLoan(Long loanId, double amount);

    /**
     * Get all loans associated with an account.
     *
     * @param accountNumber Account number to search loans for
     * @return List of loans associated with the account
     * @throws AccountDoesNotExistException if the account does not exist
     */
    /**
     * Retrieve all loans associated with an account.
     *
     * @param accountNumber Account number to search loans for
     * @return List of loans associated with the account
     * @throws AccountDoesNotExistException if the account does not exist
     */
    List<Loan> getLoansByAccountNumber(String accountNumber);
}
