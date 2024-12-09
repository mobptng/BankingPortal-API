package com.webapp.bankingportal.repository;

import com.webapp.bankingportal.entity.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {
    
    /**
     * Find all loans associated with a specific account number.
     *
     * @param accountNumber the account number to search for
     * @return a list of loans associated with the account number
     */
    List<Loan> findByAccount_AccountNumber(String accountNumber);

    /**
     * Find a loan by its ID.
     *
     * @param id the ID of the loan to find
     * @return an Optional containing the loan if found, empty otherwise
     */
    Optional<Loan> findById(Long id);

    /**
     * Save a new loan or update an existing one.
     *
     * @param loan the loan to save or update
     * @return the saved loan
     */
    Loan save(Loan loan);
}