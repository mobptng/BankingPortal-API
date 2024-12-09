

package com.webapp.bankingportal.service;

import com.webapp.bankingportal.entity.Account;
import com.webapp.bankingportal.entity.LoanStatus;
import com.webapp.bankingportal.entity.Transaction;
import com.webapp.bankingportal.entity.TransactionType;
import com.webapp.bankingportal.exception.InvalidAmountException;
import java.util.Date;
import com.webapp.bankingportal.entity.Loan;
import com.webapp.bankingportal.exception.AccountDoesNotExistException;
import com.webapp.bankingportal.exception.InsufficientBalanceException;
import com.webapp.bankingportal.exception.LoanNotFoundException;
import com.webapp.bankingportal.exception.NotImplementedException;
import com.webapp.bankingportal.repository.AccountRepository;
import com.webapp.bankingportal.repository.LoanRepository;
import com.webapp.bankingportal.repository.TransactionRepository;
import com.webapp.bankingportal.util.ValidationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class LoanServiceImpl implements LoanService {
    
    private final LoanRepository loanRepository;
    private final AccountRepository accountRepository;
    private final ValidationUtil validationUtil;
    private final AccountService accountService;
    private final TransactionRepository transactionRepository;

    @Override
    @Transactional
    public Loan applyForLoan(String accountNumber, double amount, String description) {
        Account account = accountRepository.findByAccountNumber(accountNumber);
        if (account == null) {
            throw new AccountDoesNotExistException("Account does not exist");
        }
        
        if (account.getBalance() <= 0) {
            throw new InsufficientBalanceException("Account must have positive balance");
        }
        
        if (amount > account.getBalance() * 2) {
            throw new InvalidAmountException("Loan amount cannot exceed twice the account balance");
        }
        
        Loan loan = new Loan(account, amount, description);
        loan.setInterestRate(5.0); // 5% fixed interest rate
        loan.setRepaymentPeriod(12); // 1 year fixed period
        loan.setOutstandingBalance(amount * (1 + 0.05)); // Principal + Interest
        loan.setStatus(LoanStatus.PENDING);
        
        return loanRepository.save(loan);
    }

    @Override
    @Transactional
    public Loan approveLoan(Long loanId) {
        Loan loan = loanRepository.findById(loanId)
            .orElseThrow(() -> new LoanNotFoundException("Loan not found"));
            
        if (loan.getStatus() != LoanStatus.PENDING) {
            throw new IllegalStateException("Loan is not in PENDING status");
        }
        
        loan.setStatus(LoanStatus.APPROVED);
        
        // Create transaction for loan disbursement
        Transaction transaction = new Transaction();
        transaction.setAmount(loan.getAmount());
        transaction.setTransactionType(TransactionType.LOAN_DISBURSEMENT);
        transaction.setTransactionDate(new Date());
        transaction.setTargetAccount(loan.getAccount());
        
        // Update account balance
        Account account = loan.getAccount();
        account.setBalance(account.getBalance() + loan.getAmount());
        accountRepository.save(account);
        transactionRepository.save(transaction);
        
        return loanRepository.save(loan);
    }

    @Override
    @Transactional
    public void repayLoan(Long loanId, double amount) {
        Loan loan = loanRepository.findById(loanId)
            .orElseThrow(() -> new LoanNotFoundException("Loan not found"));
            
        if (loan.getStatus() != LoanStatus.APPROVED) {
            throw new IllegalStateException("Loan is not in APPROVED status");
        }
        
        if (amount > loan.getOutstandingBalance()) {
            throw new InvalidAmountException("Repayment amount exceeds outstanding balance");
        }
        
        Account account = loan.getAccount();
        if (account.getBalance() < amount) {
            throw new InsufficientBalanceException("Insufficient balance for loan repayment");
        }
        
        // Update loan outstanding balance
        loan.setOutstandingBalance(loan.getOutstandingBalance() - amount);
        if (loan.getOutstandingBalance() == 0) {
            loan.setStatus(LoanStatus.REPAID);
        }
        
        // Create transaction for loan repayment
        Transaction transaction = new Transaction();
        transaction.setAmount(amount);
        transaction.setTransactionType(TransactionType.LOAN_REPAYMENT);
        transaction.setTransactionDate(new Date());
        transaction.setSourceAccount(account);
        
        // Update account balance
        account.setBalance(account.getBalance() - amount);
        accountRepository.save(account);
        transactionRepository.save(transaction);
        
        loanRepository.save(loan);
    }

    @Override
    public List<Loan> getLoansByAccountNumber(String accountNumber) {
        if (!validationUtil.doesAccountExist(accountNumber)) {
            throw new AccountDoesNotExistException("Account does not exist");
        }
        return loanRepository.findByAccount_AccountNumber(accountNumber);
    }
}