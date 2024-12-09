package com.webapp.bankingportal.service;

/* Begin section: imports */
import com.webapp.bankingportal.entity.Account;
import com.webapp.bankingportal.entity.Loan;
import com.webapp.bankingportal.entity.LoanStatus;
import com.webapp.bankingportal.entity.Transaction;
import com.webapp.bankingportal.entity.TransactionType;
import com.webapp.bankingportal.exception.AccountDoesNotExistException;
import com.webapp.bankingportal.exception.InsufficientBalanceException;
import com.webapp.bankingportal.exception.InvalidAmountException;
import com.webapp.bankingportal.exception.LoanNotFoundException;
import com.webapp.bankingportal.repository.AccountRepository;
import com.webapp.bankingportal.repository.LoanRepository;
import com.webapp.bankingportal.repository.TransactionRepository;
import com.webapp.bankingportal.util.ValidationUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;
/* End section: imports */

@ExtendWith(MockitoExtension.class)
public class LoanServiceImplTest {

    /* Begin section: class member variables */
    @Mock
    private LoanRepository loanRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private ValidationUtil validationUtil;

    @Mock
    private AccountService accountService;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private LoanServiceImpl loanService;
    /* End section: class member variables */

    /* Begin section: setup */
    @BeforeEach
    void setUp() {
        // Additional setup if needed
    }
    /* End section: setup */

    /* Begin section: tests */  
  
    @Test
    void shouldSuccessfullyApplyForLoan() {
        // Given
        String accountNumber = "123456";
        double amount = 500.0;
        String description = "Personal Loan";
        
        Account account = new Account();
        account.setAccountNumber(accountNumber);
        account.setBalance(1000.0);
        
        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(account);
        when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        Loan result = loanService.applyForLoan(accountNumber, amount, description);
        
        // Then
        assertNotNull(result);
        assertEquals(amount, result.getAmount());
        assertEquals(5.0, result.getInterestRate());
        assertEquals(12, result.getRepaymentPeriod());
        assertEquals(525.0, result.getOutstandingBalance());
        assertEquals(description, result.getDescription());
        assertEquals(LoanStatus.PENDING, result.getStatus());
        assertEquals(account, result.getAccount());
        
        verify(loanRepository).save(any(Loan.class));
    }  
  
    @Test
    void applyForLoan_whenAccountDoesNotExist_thenThrowException() {
        // Given
        String accountNumber = "999999";
        double amount = 500.0;
        String description = "Personal Loan";
        
        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(null);

        // When & Then
        AccountDoesNotExistException exception = assertThrows(
            AccountDoesNotExistException.class,
            () -> loanService.applyForLoan(accountNumber, amount, description)
        );
        
        assertEquals("Account does not exist", exception.getMessage());
        verify(accountRepository).findByAccountNumber(accountNumber);
        verifyNoMoreInteractions(accountRepository);
        verifyNoInteractions(loanRepository);
    }  
  
    @Test
    void applyForLoan_shouldThrowInsufficientBalanceException_whenAccountBalanceIsZero() {
        // Given
        String accountNumber = "123456";
        double amount = 500.0;
        String description = "Personal Loan";
        
        Account account = new Account();
        account.setAccountNumber(accountNumber);
        account.setBalance(0.0);
        
        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(account);
        
        // When & Then
        InsufficientBalanceException exception = assertThrows(
            InsufficientBalanceException.class,
            () -> loanService.applyForLoan(accountNumber, amount, description)
        );
        
        assertEquals("Account must have positive balance", exception.getMessage());
        verify(accountRepository).findByAccountNumber(accountNumber);
        verifyNoInteractions(loanRepository);
    }  
  
    @Test
    void applyForLoan_shouldThrowException_whenLoanAmountExceedsTwiceBalance() {
        // Given
        String accountNumber = "123456";
        double accountBalance = 1000.0;
        double loanAmount = 2500.0;
        String description = "Large Loan";

        Account account = new Account();
        account.setAccountNumber(accountNumber);
        account.setBalance(accountBalance);

        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(account);

        // When & Then
        InvalidAmountException exception = assertThrows(InvalidAmountException.class, () -> 
            loanService.applyForLoan(accountNumber, loanAmount, description)
        );
        
        assertEquals("Loan amount cannot exceed twice the account balance", exception.getMessage());
        verify(accountRepository).findByAccountNumber(accountNumber);
        verifyNoInteractions(loanRepository);
    }  
  
    @Test
    void shouldSuccessfullyApprovePendingLoan() {
        // Given
        Long loanId = 1L;
        Account account = new Account();
        account.setBalance(5000.0);
        
        Loan loan = new Loan();
        loan.setId(loanId);
        loan.setStatus(LoanStatus.PENDING);
        loan.setAmount(1000.0);
        loan.setAccount(account);
        
        when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
        when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        Loan result = loanService.approveLoan(loanId);
        
        // Then
        assertEquals(LoanStatus.APPROVED, result.getStatus());
        assertEquals(6000.0, result.getAccount().getBalance());
        
        verify(accountRepository).save(argThat(savedAccount -> 
            savedAccount.getBalance() == 6000.0
        ));
        
        verify(loanRepository).save(argThat(savedLoan -> 
            savedLoan.getStatus() == LoanStatus.APPROVED &&
            savedLoan.getAmount() == 1000.0
        ));
        
        ArgumentCaptor<Loan> loanCaptor = ArgumentCaptor.forClass(Loan.class);
        verify(loanRepository).save(loanCaptor.capture());
        Loan savedLoan = loanCaptor.getValue();
        
        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(transactionCaptor.capture());
        Transaction transaction = transactionCaptor.getValue();
        assertNotNull(transaction);
        assertEquals(1000.0, transaction.getAmount());
        assertEquals(TransactionType.LOAN_DISBURSEMENT, transaction.getTransactionType());
        assertEquals(account, transaction.getTargetAccount());
        assertNotNull(transaction.getTransactionDate());
    }  
  
    @Test
    void approveLoan_whenLoanNotFound_shouldThrowLoanNotFoundException() {
        // Given
        Long nonExistentLoanId = 2L;
        when(loanRepository.findById(nonExistentLoanId)).thenReturn(Optional.empty());

        // When & Then
        LoanNotFoundException exception = assertThrows(LoanNotFoundException.class, 
            () -> loanService.approveLoan(nonExistentLoanId));
        assertEquals("Loan not found", exception.getMessage());
        
        verify(loanRepository).findById(nonExistentLoanId);
        verifyNoMoreInteractions(loanRepository);
        verifyNoInteractions(accountRepository);
    }  
  
    @Test
    void approveLoan_whenLoanNotInPendingStatus_throwsIllegalStateException() {
        // Given
        Long loanId = 3L;
        Loan loan = new Loan();
        loan.setId(loanId);
        loan.setStatus(LoanStatus.APPROVED);
        
        when(loanRepository.findById(loanId))
            .thenReturn(Optional.of(loan));

        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> loanService.approveLoan(loanId));
        
        assertEquals("Loan is not in PENDING status", exception.getMessage());
        verify(loanRepository).findById(loanId);
        verifyNoMoreInteractions(loanRepository);
        verifyNoInteractions(accountRepository);
    }  
  
    @Test
    void repayLoan_shouldSuccessfullyRepayLoanAndUpdateBalances() {
        // Arrange
        Long loanId = 1L;
        double repaymentAmount = 500.0;
        double initialAccountBalance = 600.0;
        
        Account account = new Account();
        account.setBalance(initialAccountBalance);
        
        Loan loan = new Loan();
        loan.setId(loanId);
        loan.setStatus(LoanStatus.APPROVED);
        loan.setOutstandingBalance(repaymentAmount);
        loan.setAccount(account);
        
        when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
        when(loanRepository.save(any(Loan.class))).thenAnswer(i -> i.getArguments()[0]);
        when(accountRepository.save(any(Account.class))).thenAnswer(i -> i.getArguments()[0]);
        
        // Act
        loanService.repayLoan(loanId, repaymentAmount);
        
        // Assert
        assertEquals(0.0, loan.getOutstandingBalance());
        assertEquals(LoanStatus.REPAID, loan.getStatus());
        assertEquals(100.0, account.getBalance());
        
        verify(loanRepository).findById(loanId);
        verify(accountRepository).save(account);
        verify(loanRepository).save(loan);
        
        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(transactionCaptor.capture());
        
        Transaction capturedTransaction = transactionCaptor.getValue();
        assertEquals(repaymentAmount, capturedTransaction.getAmount());
        assertEquals(TransactionType.LOAN_REPAYMENT, capturedTransaction.getTransactionType());
        assertEquals(account, capturedTransaction.getSourceAccount());
        assertNotNull(capturedTransaction.getTransactionDate());
    }  
  
    @Test
    void repayLoan_whenLoanNotFound_throwsLoanNotFoundException() {
        // Given
        Long loanId = 2L;
        double amount = 100.0;
        when(loanRepository.findById(loanId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(LoanNotFoundException.class, () -> {
            loanService.repayLoan(loanId, amount);
        });
        
        verify(loanRepository).findById(loanId);
        verifyNoMoreInteractions(loanRepository, accountRepository);
    }  
  
    @Test
    void repayLoan_shouldThrowException_whenLoanNotApproved() {
        // Given
        Long loanId = 3L;
        double amount = 100.0;
        
        Loan loan = new Loan();
        loan.setId(loanId);
        loan.setStatus(LoanStatus.PENDING);
        
        when(loanRepository.findById(loanId))
            .thenReturn(Optional.of(loan));
        
        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> loanService.repayLoan(loanId, amount));
            
        assertEquals("Loan is not in APPROVED status", exception.getMessage());
        
        verify(loanRepository).findById(loanId);
        verifyNoMoreInteractions(loanRepository, accountRepository);
    }  
  
    @Test
    void repayLoan_shouldThrowException_whenRepaymentAmountExceedsOutstandingBalance() {
        // Given
        Long loanId = 4L;
        Account account = new Account();
        account.setBalance(500.0);  // Account has sufficient balance, but that's not the issue here
        
        Loan loan = new Loan();
        loan.setId(loanId);
        loan.setStatus(LoanStatus.APPROVED);
        loan.setOutstandingBalance(300.0);
        loan.setAccount(account);
        
        when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
        
        // When & Then
        InvalidAmountException exception = assertThrows(
            InvalidAmountException.class,
            () -> loanService.repayLoan(loanId, 400.0)
        );
        
        assertEquals("Repayment amount exceeds outstanding balance", exception.getMessage());
        
        // Verify that save methods were never called
        verify(loanRepository, never()).save(any());
        verify(accountRepository, never()).save(any());
    }  
  
    @Test
    void repayLoan_shouldThrowInsufficientBalanceException_whenAccountBalanceIsLessThanRepaymentAmount() {
        // Given
        Long loanId = 5L;
        double repaymentAmount = 200.0;
        
        Account account = new Account();
        account.setBalance(150.0);
        
        Loan loan = new Loan();
        loan.setId(loanId);
        loan.setStatus(LoanStatus.APPROVED);
        loan.setOutstandingBalance(200.0);
        loan.setAccount(account);
        
        when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
        
        // When & Then
        InsufficientBalanceException exception = assertThrows(InsufficientBalanceException.class,
            () -> loanService.repayLoan(loanId, repaymentAmount));
        
        assertEquals("Insufficient balance for loan repayment", exception.getMessage());
        
        // Verify no saves were performed
        verify(accountRepository, never()).save(any());
        verify(loanRepository, never()).save(any());
    }  
  
    @Test
    void getLoansByAccountNumber_WhenAccountExists_ReturnsLoansList() {
        // Given
        String accountNumber = "123456";
        List<Loan> expectedLoans = Arrays.asList(
            new Loan(),
            new Loan()
        );
        
        when(validationUtil.doesAccountExist(accountNumber)).thenReturn(true);
        when(loanRepository.findByAccount_AccountNumber(accountNumber)).thenReturn(expectedLoans);

        // When
        List<Loan> actualLoans = loanService.getLoansByAccountNumber(accountNumber);

        // Then
        assertThat(actualLoans).isEqualTo(expectedLoans);
        verify(validationUtil).doesAccountExist(accountNumber);
        verify(loanRepository).findByAccount_AccountNumber(accountNumber);
    }  
  
    @Test
    void getLoansByAccountNumber_WhenAccountExistsWithoutLoans_ReturnsEmptyList() {
        // Given
        String accountNumber = "654321";
        when(validationUtil.doesAccountExist(accountNumber)).thenReturn(true);
        when(loanRepository.findByAccount_AccountNumber(accountNumber)).thenReturn(Collections.emptyList());

        // When
        List<Loan> result = loanService.getLoansByAccountNumber(accountNumber);

        // Then
        assertThat(result).isEmpty();
        verify(validationUtil).doesAccountExist(accountNumber);
        verify(loanRepository).findByAccount_AccountNumber(accountNumber);
    }  
  
    @Test
    void shouldThrowAccountDoesNotExistExceptionWhenAccountNumberDoesNotExist() {
        // Given
        String nonExistentAccountNumber = "000000";
        when(validationUtil.doesAccountExist(nonExistentAccountNumber)).thenReturn(false);

        // When & Then
        AccountDoesNotExistException exception = assertThrows(
            AccountDoesNotExistException.class,
            () -> loanService.getLoansByAccountNumber(nonExistentAccountNumber)
        );
        
        assertEquals("Account does not exist", exception.getMessage());
        verify(validationUtil).doesAccountExist(nonExistentAccountNumber);
        verifyNoInteractions(loanRepository);
    }

    /* End section: tests */
}