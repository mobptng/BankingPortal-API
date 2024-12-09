
package com.webapp.bankingportal.controller;

/* Begin section: imports */
import com.fasterxml.jackson.databind.ObjectMapper;
import com.webapp.bankingportal.dto.LoanRequest;
import com.webapp.bankingportal.entity.LoanStatus;
import com.webapp.bankingportal.dto.LoanResponse;
import com.webapp.bankingportal.entity.Loan;
import com.webapp.bankingportal.exception.AccountNotFoundException;
import com.webapp.bankingportal.exception.AmountExceedLimitException;
import com.webapp.bankingportal.exception.InvalidAmountException;
import com.webapp.bankingportal.exception.LoanAlreadyApprovedException;
import com.webapp.bankingportal.exception.LoanApprovalNotAllowedException;
import com.webapp.bankingportal.exception.LoanNotFoundException;
import com.webapp.bankingportal.exception.InsufficientFundsException;
import com.webapp.bankingportal.service.LoanService;
import com.webapp.bankingportal.util.ApiMessages;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;
/* End section: imports */

@ExtendWith(MockitoExtension.class)
public class LoanControllerTest {

    /* Begin section: class member variables */
    @Mock
    private LoanService loanService;

    @InjectMocks
    private LoanController loanController;

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;
    /* End section: class member variables */


    /* Begin section: setup */
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(loanController)
                .build();
        objectMapper = new ObjectMapper();
    }
    /* End section: setup */


    /* Begin section: tests */  
  
    @Test
    void applyForLoan_WithValidRequest_ShouldReturnSuccessResponse() throws Exception {
        // Given
        LoanRequest loanRequest = new LoanRequest();
        loanRequest.setAccountNumber("123456");
        loanRequest.setAmount(5000);
        loanRequest.setDescription("Personal Loan");

        Loan mockLoan = new Loan();
        when(loanService.applyForLoan(
            loanRequest.getAccountNumber(),
            loanRequest.getAmount(),
            loanRequest.getDescription()
        )).thenReturn(mockLoan);

        // When & Then
        mockMvc.perform(post("/api/loans/apply")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loanRequest)))
                .andExpect(status().isOk())
                .andExpect(content().string(ApiMessages.LOAN_APPLICATION_SUCCESS.getMessage()));

        verify(loanService).applyForLoan(
            loanRequest.getAccountNumber(),
            loanRequest.getAmount(),
            loanRequest.getDescription()
        );
    }  
  
    @Test
    void applyForLoan_WithInvalidAccountNumber_ShouldThrowAccountNotFoundException() throws Exception {
        // Given
        String invalidAccountNumber = "invalid123";
        LoanRequest loanRequest = new LoanRequest();
        loanRequest.setAccountNumber(invalidAccountNumber);
        loanRequest.setAmount(5000.0);
        loanRequest.setDescription("Personal Loan");

        when(loanService.applyForLoan(
            eq(invalidAccountNumber),
            eq(5000.0),
            eq("Personal Loan")
        )).thenThrow(new AccountNotFoundException(String.format("Account not found: %s", invalidAccountNumber)));

        // When & Then
        mockMvc.perform(post("/api/loans/apply")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loanRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Account not found: invalid123"));
    }  
  
    @Test
    void applyForLoan_WhenAmountExceedsLimit_ThenThrowsException() throws Exception {
        // Given
        LoanRequest loanRequest = new LoanRequest();
        loanRequest.setAccountNumber("123456");
        loanRequest.setAmount(150000);
        loanRequest.setDescription("Business Loan");

        when(loanService.applyForLoan(
            loanRequest.getAccountNumber(),
            loanRequest.getAmount(),
            loanRequest.getDescription()
        )).thenThrow(new AmountExceedLimitException(ApiMessages.AMOUNT_EXCEED_100_000_ERROR.getMessage()));

        // When & Then
        mockMvc.perform(post("/api/loans/apply")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loanRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString(ApiMessages.AMOUNT_EXCEED_100_000_ERROR.getMessage())));

        verify(loanService).applyForLoan(
            loanRequest.getAccountNumber(),
            loanRequest.getAmount(),
            loanRequest.getDescription()
        );
    }  
  
    @Test
    void applyForLoan_WithNegativeAmount_ShouldThrowException() throws Exception {
        // Arrange
        LoanRequest loanRequest = new LoanRequest();
        loanRequest.setAccountNumber("123456");
        loanRequest.setAmount(-5000);
        loanRequest.setDescription("Negative Test");

        when(loanService.applyForLoan(
            loanRequest.getAccountNumber(),
            loanRequest.getAmount(),
            loanRequest.getDescription()
        )).thenThrow(new InvalidAmountException(ApiMessages.AMOUNT_NEGATIVE_ERROR.getMessage()));

        // Act & Assert
        mockMvc.perform(post("/api/loans/apply")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loanRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString(ApiMessages.AMOUNT_NEGATIVE_ERROR.getMessage())));
    }  
  
    @Test
    void applyForLoan_WithNullDescription_ShouldReturnSuccessMessage() throws Exception {
        // Arrange
        String accountNumber = "123456";
        double amount = 5000.0;
        LoanRequest loanRequest = new LoanRequest();
        loanRequest.setAccountNumber(accountNumber);
        loanRequest.setAmount(amount);
        loanRequest.setDescription(null);

        Loan mockLoan = new Loan();
        when(loanService.applyForLoan(accountNumber, amount, null))
            .thenReturn(mockLoan);

        // Act & Assert
        mockMvc.perform(post("/api/loans/apply")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loanRequest)))
                .andExpect(status().isOk())
                .andExpect(content().string(ApiMessages.LOAN_APPLICATION_SUCCESS.getMessage()));

        verify(loanService).applyForLoan(accountNumber, amount, null);
    }  
  
    @Test
    void approveLoan_ShouldReturnSuccessMessage_WhenLoanApprovalIsSuccessful() throws Exception {
        // Given
        Long loanId = 1L;
        doNothing().when(loanService).approveLoan(loanId);

        // When & Then
        mockMvc.perform(post("/api/loans/approve/{loanId}", loanId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(ApiMessages.LOAN_APPROVAL_SUCCESS.getMessage()));

        verify(loanService, times(1)).approveLoan(loanId);
    }  
  
    @Test
    void approveLoan_WhenLoanNotFound_ShouldReturnNotFound() throws Exception {
        // Given
        Long nonExistentLoanId = 999L;
        when(loanService.approveLoan(nonExistentLoanId))
            .thenThrow(new LoanNotFoundException("Loan not found with ID: " + nonExistentLoanId));

        // When & Then
        mockMvc.perform(post("/api/loans/approve/{loanId}", nonExistentLoanId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("Loan not found")));
    }  
  
    @Test
    void approveLoan_whenLoanAlreadyApproved_shouldReturnBadRequest() throws Exception {
        // Given
        Long loanId = 1L;
        doThrow(new LoanAlreadyApprovedException("Loan is already approved"))
            .when(loanService).approveLoan(loanId);

        // When & Then
        mockMvc.perform(post("/api/loans/approve/{loanId}", loanId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("already approved")));
    }  
  
    @Test
    void approveLoan_whenApprovalNotAllowed_shouldReturnForbiddenStatus() throws Exception {
        // Given
        Long loanId = 1L;
        String errorMessage = "Loan approval not allowed due to business rules";
        doThrow(new LoanApprovalNotAllowedException(errorMessage))
            .when(loanService).approveLoan(loanId);

        // When & Then
        mockMvc.perform(post("/api/loans/approve/{loanId}", loanId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(content().string(containsString(errorMessage)));
    }  
  
    @Test
    void approveLoan_WhenServiceThrowsException_ThenReturns500() throws Exception {
        // Given
        Long loanId = 1L;
        doThrow(new RuntimeException("Unexpected error")).when(loanService).approveLoan(loanId);

        // When & Then
        mockMvc.perform(post("/api/loans/approve/{loanId}", loanId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(containsString("unexpected error")));
    }  
  
    @Test
    void repayLoan_ShouldReturnSuccessMessage_WhenRepaymentIsSuccessful() throws Exception {
        // Given
        Long loanId = 1L;
        double amount = 1000.0;
        doNothing().when(loanService).repayLoan(loanId, amount);

        // When & Then
        mockMvc.perform(post("/api/loans/repay/{loanId}", loanId)
                .param("amount", String.valueOf(amount))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(ApiMessages.LOAN_REPAYMENT_SUCCESS.getMessage()));

        verify(loanService).repayLoan(loanId, amount);
    }  
  
    @Test
    void repayLoan_WhenLoanNotFound_ThrowsLoanNotFoundException() throws Exception {
        // Given
        Long loanId = 1L;
        double amount = 1000.0;
        doThrow(new LoanNotFoundException("Loan not found"))
            .when(loanService)
            .repayLoan(loanId, amount);

        // When & Then
        mockMvc.perform(post("/api/loans/repay/{loanId}", loanId)
                .param("amount", String.valueOf(amount))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof LoanNotFoundException))
                .andExpect(result -> assertEquals("Loan not found", 
                    result.getResolvedException().getMessage()));
    }  
  
    @Test
    void repayLoan_WithInvalidAmount_ShouldThrowInvalidAmountException() throws Exception {
        // Given
        Long loanId = 1L;
        double invalidAmount = -100.0;
        doThrow(new InvalidAmountException("Amount must be greater than 0"))
            .when(loanService)
            .repayLoan(loanId, invalidAmount);

        // When & Then
        mockMvc.perform(post("/api/loans/repay/{loanId}", loanId)
                .param("amount", String.valueOf(invalidAmount))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof InvalidAmountException))
                .andExpect(result -> assertEquals("Amount must be greater than 0", 
                    result.getResolvedException().getMessage()));
    }  
  
    @Test
    void repayLoan_whenInsufficientFunds_thenThrowsInsufficientFundsException() throws Exception {
        // Given
        Long loanId = 1L;
        double amount = 1000.0;
        doThrow(new InsufficientFundsException("Insufficient funds"))
            .when(loanService)
            .repayLoan(loanId, amount);

        // When & Then
        mockMvc.perform(post("/api/loans/repay/{loanId}", loanId)
                .param("amount", String.valueOf(amount))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof InsufficientFundsException));
    }  
  
    @Test
    void repayLoan_WhenServiceThrowsRuntimeException_ThenExceptionIsThrown() throws Exception {
        // Given
        Long loanId = 1L;
        double amount = 1000.0;
        doThrow(new RuntimeException("Unexpected error"))
            .when(loanService)
            .repayLoan(loanId, amount);

        // When/Then
        mockMvc.perform(post("/api/loans/repay/{loanId}", loanId)
                .param("amount", String.valueOf(amount))
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isInternalServerError());
    }  
  
    @Test
    void getLoansByAccount_WithValidAccountNumber_ReturnsLoansList() {
        // Given
        String accountNumber = "12345";
        List<Loan> mockLoans = List.of(
            Loan.builder()
                .id(1L)
                .amount(10000.0)
                .interestRate(5.0)
                .repaymentPeriod(12)
                .outstandingBalance(8000.0)
                .description("Home Loan")
                .status(LoanStatus.APPROVED)
                .build(),
            Loan.builder()
                .id(2L)
                .amount(5000.0)
                .interestRate(7.0)
                .repaymentPeriod(6)
                .outstandingBalance(3000.0)
                .description("Personal Loan")
                .status(LoanStatus.APPROVED)
                .build()
        );
        
        when(loanService.getLoansByAccountNumber(accountNumber)).thenReturn(mockLoans);

        // When
        ResponseEntity<List<LoanResponse>> response = loanController.getLoansByAccount(accountNumber);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(2);
        
        LoanResponse firstLoan = response.getBody().get(0);
        assertThat(firstLoan.getId()).isEqualTo(1L);
        assertThat(firstLoan.getAmount()).isEqualTo(10000.0);
        assertThat(firstLoan.getInterestRate()).isEqualTo(5.0);
        assertThat(firstLoan.getRepaymentPeriod()).isEqualTo(12);
        assertThat(firstLoan.getOutstandingBalance()).isEqualTo(8000.0);
        assertThat(firstLoan.getDescription()).isEqualTo("Home Loan");
        assertThat(firstLoan.getStatus()).isEqualTo("ACTIVE");
        
        verify(loanService).getLoansByAccountNumber(accountNumber);
    }  
  
    @Test
    void getLoansByAccount_WhenNoLoansFound_ReturnsEmptyList() {
        // Given
        String accountNumber = "67890";
        when(loanService.getLoansByAccountNumber(accountNumber)).thenReturn(Collections.emptyList());

        // When
        ResponseEntity<List<LoanResponse>> response = loanController.getLoansByAccount(accountNumber);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());
        
        verify(loanService).getLoansByAccountNumber(accountNumber);
    }  
  
    @Test
    void getLoansByAccount_WithInvalidAccountNumber_ReturnsEmptyList() {
        // Given
        String invalidAccountNumber = "invalidAccount";
        when(loanService.getLoansByAccountNumber(invalidAccountNumber)).thenReturn(Collections.emptyList());

        // When
        ResponseEntity<List<LoanResponse>> response = loanController.getLoansByAccount(invalidAccountNumber);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());
        verify(loanService).getLoansByAccountNumber(invalidAccountNumber);
    }  
  
    @Test
    void getLoansByAccount_shouldPropagateRuntimeException_whenServiceThrowsException() {
        // Given
        String accountNumber = "54321";
        when(loanService.getLoansByAccountNumber(accountNumber))
            .thenThrow(new RuntimeException("Service layer error"));

        // When & Then
        assertThrows(RuntimeException.class, () -> 
            loanController.getLoansByAccount(accountNumber));
    }

    /* End section: tests */

}