package com.webapp.bankingportal.controller;

/* Begin section: imports */
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.view.json.MappingJackson2JsonView;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.webapp.bankingportal.dto.AmountRequest;
import com.webapp.bankingportal.dto.FundTransferRequest;
import com.webapp.bankingportal.dto.PinRequest;
import com.webapp.bankingportal.dto.PinUpdateRequest;
import com.webapp.bankingportal.dto.TransactionDTO;
import com.webapp.bankingportal.dto.TransactionType;
import com.webapp.bankingportal.exception.AccountDoesNotExistException;
import com.webapp.bankingportal.exception.AccountInactiveException;
import com.webapp.bankingportal.exception.DatabaseException;
import com.webapp.bankingportal.exception.FundTransferException;
import com.webapp.bankingportal.exception.InsufficientBalanceException;
import com.webapp.bankingportal.exception.InvalidAmountException;
import com.webapp.bankingportal.exception.InvalidPasswordException;
import com.webapp.bankingportal.exception.InvalidPinException;
import com.webapp.bankingportal.exception.NetworkException;
import com.webapp.bankingportal.exception.NetworkFailureException;
import com.webapp.bankingportal.exception.PinAlreadyExistsException;
import com.webapp.bankingportal.service.AccountService;
import com.webapp.bankingportal.service.TransactionService;
import com.webapp.bankingportal.util.ApiMessages;
import com.webapp.bankingportal.util.JsonUtil;
import com.webapp.bankingportal.util.LoggedinUser;
/* End section: imports */

@ExtendWith(MockitoExtension.class)
public class AccountControllerTest {

    /* Begin section: class member variables */
    @Mock
    private AccountService accountService;

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private AccountController accountController;

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;
    /* End section: class member variables */


    /* Begin section: setup */
    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = standaloneSetup(accountController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setViewResolvers((viewName, locale) -> new MappingJackson2JsonView())
                .build();
    }
    /* End section: setup */


    /* Begin section: tests */  
  
    @Test
    void checkAccountPIN_whenPinIsCreated_returnsSuccessMessage() throws Exception {
        // Given
        try (MockedStatic<LoggedinUser> mockedStatic = mockStatic(LoggedinUser.class)) {
            mockedStatic.when(LoggedinUser::getAccountNumber).thenReturn("123456789");
            when(accountService.isPinCreated("123456789")).thenReturn(true);

            // When & Then
            mockMvc.perform(get("/api/account/pin/check"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("PIN has been created for this account"));
        }
    }  
  
    @Test
    void checkAccountPIN_whenPinNotCreated_returnsExpectedResponse() throws Exception {
        // Given
        try (MockedStatic<LoggedinUser> mockedStatic = mockStatic(LoggedinUser.class)) {
            mockedStatic.when(LoggedinUser::getAccountNumber).thenReturn("123456789");
            when(accountService.isPinCreated("123456789")).thenReturn(false);

            // When & Then
            mockMvc.perform(get("/api/account/pin/check"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("PIN has not been created for this account"));
        }
    }  
  
    @Test
    void checkAccountPIN_WhenLoggedInUserThrowsException_ThenExceptionIsThrown() throws Exception {
        try (MockedStatic<LoggedinUser> mockedStatic = mockStatic(LoggedinUser.class)) {
            // Given
            mockedStatic.when(LoggedinUser::getAccountNumber)
                    .thenThrow(new RuntimeException("Failed to get account number"));

            // When & Then
            mockMvc.perform(get("/api/account/pin/check"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(result -> assertTrue(result.getResolvedException() instanceof RuntimeException))
                    .andExpect(result -> assertEquals("Failed to get account number", 
                            result.getResolvedException().getMessage()));
        }
    }  
  
    @Test
    void checkAccountPIN_shouldThrowException_whenAccountServiceThrowsException() throws Exception {
        // Given
        try (MockedStatic<LoggedinUser> mockedStatic = mockStatic(LoggedinUser.class)) {
            mockedStatic.when(LoggedinUser::getAccountNumber).thenReturn("123456789");
            when(accountService.isPinCreated("123456789")).thenThrow(new RuntimeException("Service error"));

            // When & Then
            mockMvc.perform(get("/api/account/pin/check"))
                    .andExpect(status().isInternalServerError());
        }
    }  
  
    @Test
    void checkAccountPIN_whenAccountNumberIsNull_shouldThrowException() {
        try (MockedStatic<LoggedinUser> mockedStatic = mockStatic(LoggedinUser.class)) {
            // Given
            mockedStatic.when(LoggedinUser::getAccountNumber).thenReturn(null);

            // When & Then
            assertThrows(IllegalArgumentException.class, () -> {
                accountController.checkAccountPIN();
            });
        }
    }  
  
    @Test
    void createPIN_shouldReturnSuccessResponse_whenValidRequest() throws Exception {
        // Given
        String accountNumber = "1234567890";
        String password = "validPassword";
        String pin = "1234";
        PinRequest pinRequest = new PinRequest(accountNumber, pin, password);

        try (MockedStatic<LoggedinUser> mockedStatic = mockStatic(LoggedinUser.class)) {
            mockedStatic.when(LoggedinUser::getAccountNumber).thenReturn(accountNumber);
            doNothing().when(accountService).createPin(accountNumber, password, pin);

            // When
            mockMvc.perform(post("/api/account/pin/create")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(pinRequest)))
                    // Then
                    .andExpect(status().isOk())
                    .andExpect(content().string(ApiMessages.PIN_CREATION_SUCCESS.getMessage()));

            verify(accountService).createPin(accountNumber, password, pin);
        }
    }  
  
    @Test
    void createPIN_WithInvalidPassword_ShouldReturnBadRequest() throws Exception {
        // Given
        String accountNumber = "123456789";
        String invalidPassword = "wrongPassword";
        String pin = "1234";
        
        try (MockedStatic<LoggedinUser> mockedStatic = mockStatic(LoggedinUser.class)) {
            mockedStatic.when(LoggedinUser::getAccountNumber).thenReturn(accountNumber);
            
            doThrow(new InvalidPasswordException("Invalid password"))
                .when(accountService)
                .createPin(accountNumber, invalidPassword, pin);

            PinRequest pinRequest = new PinRequest(accountNumber, pin, invalidPassword);
            
            // When & Then
            mockMvc.perform(post("/api/account/pin/create")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(pinRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string(containsString("Invalid password")));
        }
    }  
  
    @Test
    void createPIN_whenInvalidPinFormat_thenReturnsBadRequest() throws Exception {
        // Given
        String accountNumber = "1234567890";
        String password = "validPassword";
        String invalidPin = "12"; // Invalid PIN format (too short)
        
        try (MockedStatic<LoggedinUser> mockedStatic = mockStatic(LoggedinUser.class)) {
            mockedStatic.when(LoggedinUser::getAccountNumber).thenReturn(accountNumber);
            
            PinRequest pinRequest = new PinRequest(accountNumber, invalidPin, password);
            
            doThrow(new InvalidPinException("PIN must be 4 digits"))
                .when(accountService)
                .createPin(accountNumber, password, invalidPin);

            // When & Then
            mockMvc.perform(post("/api/account/pin/create")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(pinRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string(containsString("PIN must be 4 digits")));
        }
    }  
  
    @Test
    void createPIN_whenPinAlreadyExists_shouldReturnError() throws Exception {
        // Given
        String accountNumber = "1234567890";
        String password = "testPassword";
        String pin = "1234";
        
        try (MockedStatic<LoggedinUser> mockedStatic = mockStatic(LoggedinUser.class)) {
            mockedStatic.when(LoggedinUser::getAccountNumber).thenReturn(accountNumber);
            
            PinRequest pinRequest = new PinRequest(accountNumber, pin, password);
            
            doThrow(new PinAlreadyExistsException(ApiMessages.PIN_ALREADY_EXISTS.getMessage()))
                .when(accountService)
                .createPin(accountNumber, password, pin);

            // When & Then
            mockMvc.perform(post("/api/account/pin/create")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(pinRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string(ApiMessages.PIN_ALREADY_EXISTS.getMessage()));
        }
    }  
  
    @Test
    void createPIN_whenServiceLayerFails_thenReturnsErrorResponse() throws Exception {
        // Given
        String accountNumber = "1234567890";
        PinRequest pinRequest = new PinRequest(accountNumber, "1234", "password123");
        
        try (MockedStatic<LoggedinUser> mockedStatic = mockStatic(LoggedinUser.class)) {
            mockedStatic.when(LoggedinUser::getAccountNumber).thenReturn(accountNumber);
            
            doThrow(new RuntimeException("Database error"))
                .when(accountService)
                .createPin(accountNumber, pinRequest.password(), pinRequest.pin());

            // When & Then
            mockMvc.perform(post("/api/account/pin/create")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(pinRequest)))
                    .andExpect(status().isInternalServerError())
                    .andExpect(content().string(containsString("Database error")));
        }
    }  
  
    @Test
    void updatePIN_ShouldReturnSuccessMessage_WhenValidRequest() throws Exception {
        // Given
        String accountNumber = "1234567890";
        String oldPin = "1234";
        String password = "validPassword";
        String newPin = "5678";
        
        PinUpdateRequest pinUpdateRequest = new PinUpdateRequest(accountNumber, oldPin, newPin, password);
        
        try (MockedStatic<LoggedinUser> mockedStatic = mockStatic(LoggedinUser.class)) {
            mockedStatic.when(LoggedinUser::getAccountNumber).thenReturn(accountNumber);
            
            // When
            mockMvc.perform(post("/api/account/pin/update")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(pinUpdateRequest)))
                    
                    // Then
                    .andExpect(status().isOk())
                    .andExpect(content().string(ApiMessages.PIN_UPDATE_SUCCESS.getMessage()));
            
            verify(accountService).updatePin(accountNumber, oldPin, password, newPin);
        }
    }  
  
    @Test
    void updatePIN_WithIncorrectOldPin_ShouldReturnError() throws Exception {
        // Given
        String accountNumber = "1234567890";
        String oldPin = "1111";
        String newPin = "2222";
        String password = "password123";
        
        try (MockedStatic<LoggedinUser> mockedStatic = mockStatic(LoggedinUser.class)) {
            mockedStatic.when(LoggedinUser::getAccountNumber).thenReturn(accountNumber);
            
            doThrow(new InvalidPinException("Invalid PIN"))
                .when(accountService)
                .updatePin(accountNumber, oldPin, password, newPin);

            PinUpdateRequest request = new PinUpdateRequest(accountNumber, oldPin, newPin, password);

            // When & Then
            mockMvc.perform(post("/api/account/pin/update")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string("Invalid PIN"));
        }
    }  
  
    @Test
    void updatePIN_WithIncorrectPassword_ShouldReturnError() throws Exception {
        // Given
        String accountNumber = "1234567890";
        String oldPin = "1234";
        String newPin = "5678";
        String incorrectPassword = "wrongPassword";

        try (MockedStatic<LoggedinUser> mockedStatic = mockStatic(LoggedinUser.class)) {
            mockedStatic.when(LoggedinUser::getAccountNumber).thenReturn(accountNumber);

            doThrow(new BadCredentialsException("Invalid password"))
                    .when(accountService)
                    .updatePin(accountNumber, oldPin, incorrectPassword, newPin);

            PinUpdateRequest request = new PinUpdateRequest(accountNumber, oldPin, newPin, incorrectPassword);

            // When & Then
            mockMvc.perform(post("/api/account/pin/update")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().string(containsString("Invalid password")));
        }
    }  
  
    @Test
    void updatePIN_WithInvalidNewPin_ShouldReturnBadRequest() throws Exception {
        // Given
        String accountNumber = "1234567890";
        String oldPin = "1234";
        String password = "validPassword";
        String invalidNewPin = "12345"; // Invalid: more than 4 digits
        
        try (MockedStatic<LoggedinUser> mockedStatic = mockStatic(LoggedinUser.class)) {
            mockedStatic.when(LoggedinUser::getAccountNumber).thenReturn(accountNumber);
            
            doThrow(new InvalidPinException("PIN must be 4 digits"))
                .when(accountService)
                .updatePin(accountNumber, oldPin, password, invalidNewPin);

            PinUpdateRequest request = new PinUpdateRequest(accountNumber, oldPin, invalidNewPin, password);

            // When & Then
            mockMvc.perform(post("/api/account/pin/update")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string(containsString("PIN must be 4 digits")));
        }
    }  
  
    @Test
    void updatePIN_shouldReturnError_whenAccountDoesNotExist() throws Exception {
        // Given
        String accountNumber = "12345678";
        PinUpdateRequest pinUpdateRequest = new PinUpdateRequest(accountNumber, "1234", "5678", "password123");
        
        try (MockedStatic<LoggedinUser> mockedStatic = mockStatic(LoggedinUser.class)) {
            mockedStatic.when(LoggedinUser::getAccountNumber).thenReturn(accountNumber);
            
            doThrow(new AccountDoesNotExistException(ApiMessages.ACCOUNT_NOT_FOUND.getMessage()))
                .when(accountService)
                .updatePin(accountNumber, pinUpdateRequest.oldPin(), pinUpdateRequest.password(), pinUpdateRequest.newPin());

            // When & Then
            mockMvc.perform(post("/api/account/pin/update")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(pinUpdateRequest)))
                    .andExpect(status().isNotFound())
                    .andExpect(content().string(ApiMessages.ACCOUNT_NOT_FOUND.getMessage()));
        }
    }  
  
    @Test
    void cashDeposit_WithValidAmountAndPin_ShouldReturnSuccessMessage() throws Exception {
        // Given
        String accountNumber = "123456789";
        String pin = "1234";
        double amount = 100.00;
        AmountRequest amountRequest = new AmountRequest(accountNumber, pin, amount);

        try (MockedStatic<LoggedinUser> mockedStatic = mockStatic(LoggedinUser.class)) {
            mockedStatic.when(LoggedinUser::getAccountNumber).thenReturn(accountNumber);

            // When
            mockMvc.perform(post("/api/account/deposit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(amountRequest)))
                    
                    // Then
                    .andExpect(status().isOk())
                    .andExpect(content().string(ApiMessages.CASH_DEPOSIT_SUCCESS.getMessage()));
        }

        verify(accountService).cashDeposit(accountNumber, pin, amount);
    }  
  
    @Test
    void cashDeposit_WithIncorrectPin_ShouldReturnBadRequest() throws Exception {
        // Given
        String accountNumber = "123456789";
        String incorrectPin = "1234";
        double amount = 100.00;
        
        try (MockedStatic<LoggedinUser> mockedStatic = mockStatic(LoggedinUser.class)) {
            mockedStatic.when(LoggedinUser::getAccountNumber).thenReturn(accountNumber);
            
            doThrow(new InvalidPinException("Invalid PIN"))
                .when(accountService)
                .cashDeposit(accountNumber, incorrectPin, amount);

            AmountRequest amountRequest = new AmountRequest(accountNumber, incorrectPin, amount);
            
            // When & Then
            mockMvc.perform(post("/api/account/deposit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(amountRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string("Invalid PIN"));
        }
    }  
  
    @Test
    void cashDeposit_WithInvalidAmount_ShouldReturnBadRequest() throws Exception {
        // Given
        String accountNumber = "123456789";
        String pin = "1234";
        double invalidAmount = -50.00;
        
        try (MockedStatic<LoggedinUser> mockedStatic = mockStatic(LoggedinUser.class)) {
            mockedStatic.when(LoggedinUser::getAccountNumber).thenReturn(accountNumber);
            
            AmountRequest amountRequest = new AmountRequest(accountNumber, pin, invalidAmount);
            
            doThrow(new InvalidAmountException("Invalid deposit amount"))
                .when(accountService)
                .cashDeposit(accountNumber, pin, invalidAmount);

            // When & Then
            mockMvc.perform(post("/api/account/deposit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(amountRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string("Invalid deposit amount"));
        }
    }  
  
    @Test
    void cashDeposit_shouldReturnError_whenAccountIsInactive() throws Exception {
        // Given
        String accountNumber = "123456789";
        String pin = "1234";
        double amount = 100.00;
        
        try (MockedStatic<LoggedinUser> mockedStatic = mockStatic(LoggedinUser.class)) {
            mockedStatic.when(LoggedinUser::getAccountNumber).thenReturn(accountNumber);
            
            AmountRequest amountRequest = new AmountRequest(accountNumber, pin, amount);
            
            doThrow(new AccountInactiveException("Account is inactive"))
                .when(accountService)
                .cashDeposit(accountNumber, pin, amount);

            // When & Then
            mockMvc.perform(post("/api/account/deposit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(amountRequest)))
                    .andExpect(status().isForbidden())
                    .andExpect(content().string("Account is inactive"));
        }
    }  
  
    @Test
    void cashDeposit_WhenAmountExceedsLimit_ShouldReturnBadRequest() throws Exception {
        // Given
        String accountNumber = "123456789";
        String pin = "1234";
        double amount = 100000.00;
        AmountRequest amountRequest = new AmountRequest(accountNumber, pin, amount);
        
        try (MockedStatic<LoggedinUser> mockedStatic = mockStatic(LoggedinUser.class)) {
            mockedStatic.when(LoggedinUser::getAccountNumber).thenReturn(accountNumber);
            
            doThrow(new InvalidAmountException(ApiMessages.AMOUNT_EXCEED_100_000_ERROR.getMessage()))
                .when(accountService)
                .cashDeposit(accountNumber, pin, amount);

            // When & Then
            mockMvc.perform(post("/api/account/deposit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(amountRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string(containsString("Amount cannot be greater than 100,000")));
        }
    }  
  
    @Test
    void cashDeposit_whenServiceUnavailable_returnsServiceUnavailableStatus() throws Exception {
        // Arrange
        AmountRequest amountRequest = new AmountRequest("123456789", "1234", 100.00);
        String requestJson = objectMapper.writeValueAsString(amountRequest);

        try (MockedStatic<LoggedinUser> mockedStatic = mockStatic(LoggedinUser.class)) {
            mockedStatic.when(LoggedinUser::getAccountNumber).thenReturn("123456789");
            
            doThrow(new RuntimeException("Service unavailable"))
                .when(accountService)
                .cashDeposit(anyString(), anyString(), anyDouble());

            // Act & Assert
            mockMvc.perform(post("/api/account/deposit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(content().string("Service unavailable, please try again later"));
        }
    }  
  
    @Test
    void cashWithdrawal_WithValidRequest_ShouldReturnSuccessMessage() throws Exception {
        // Given
        String accountNumber = "123456789";
        String pin = "1234";
        double amount = 100.0;
        
        try (MockedStatic<LoggedinUser> mockedStatic = mockStatic(LoggedinUser.class)) {
            mockedStatic.when(LoggedinUser::getAccountNumber).thenReturn(accountNumber);
            
            AmountRequest amountRequest = new AmountRequest(accountNumber, pin, amount);
            
            // When
            doNothing().when(accountService).cashWithdrawal(accountNumber, pin, amount);
            
            mockMvc.perform(post("/api/account/withdraw")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(amountRequest)))
                    .andExpect(status().isOk())
                    .andExpect(content().string(ApiMessages.CASH_WITHDRAWAL_SUCCESS.getMessage()));
            
            // Then
            verify(accountService).cashWithdrawal(accountNumber, pin, amount);
        }
    }  
  
    @Test
    void cashWithdrawal_WithInvalidPin_ShouldReturnError() throws Exception {
        // Arrange
        String accountNumber = "123456789";
        String invalidPin = "0000";
        double amount = 100.0;
        
        AmountRequest amountRequest = new AmountRequest(accountNumber, invalidPin, amount);
        
        try (MockedStatic<LoggedinUser> mockedStatic = mockStatic(LoggedinUser.class)) {
            mockedStatic.when(LoggedinUser::getAccountNumber).thenReturn(accountNumber);
            
            doThrow(new InvalidPinException("Invalid PIN"))
                .when(accountService)
                .cashWithdrawal(accountNumber, invalidPin, amount);

            // Act & Assert
            mockMvc.perform(post("/api/account/withdraw")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(amountRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string(containsString("Invalid PIN")));
        }
    }  
  
    @Test
    void cashWithdrawal_shouldReturnError_whenInsufficientFunds() throws Exception {
        // Given
        String accountNumber = "123456789";
        String pin = "1234";
        double amount = 1000.0;
        
        AmountRequest amountRequest = new AmountRequest(accountNumber, pin, amount);
        
        try (MockedStatic<LoggedinUser> mockedStatic = mockStatic(LoggedinUser.class)) {
            mockedStatic.when(LoggedinUser::getAccountNumber).thenReturn(accountNumber);
            
            doThrow(new InsufficientBalanceException("Insufficient balance"))
                .when(accountService)
                .cashWithdrawal(accountNumber, pin, amount);

            // When & Then
            mockMvc.perform(post("/api/account/withdraw")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(amountRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string(ApiMessages.BALANCE_INSUFFICIENT_ERROR.getMessage()));
        }
    }  
  
    @Test
    void cashWithdrawal_whenNetworkError_thenReturnsErrorResponse() throws Exception {
        // Given
        String accountNumber = "123456789";
        String pin = "1234";
        double amount = 100.0;
        
        AmountRequest amountRequest = new AmountRequest(accountNumber, pin, amount);
        
        try (MockedStatic<LoggedinUser> mockedStatic = mockStatic(LoggedinUser.class)) {
            mockedStatic.when(LoggedinUser::getAccountNumber).thenReturn(accountNumber);
            
            doThrow(new NetworkException("Network error occurred during transaction"))
                .when(accountService)
                .cashWithdrawal(accountNumber, pin, amount);

            // When & Then
            mockMvc.perform(post("/api/account/withdraw")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(amountRequest)))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(content().string(containsString("Network error occurred during transaction")));
        }
    }  
  
    @Test
    void cashWithdrawal_whenDatabaseError_thenReturnsErrorResponse() throws Exception {
        // Given
        String accountNumber = "123456789";
        String pin = "1234";
        double amount = 100.0;
        
        AmountRequest amountRequest = new AmountRequest(accountNumber, pin, amount);
        
        try (MockedStatic<LoggedinUser> mockedStatic = mockStatic(LoggedinUser.class)) {
            mockedStatic.when(LoggedinUser::getAccountNumber).thenReturn(accountNumber);
            
            doThrow(new DatabaseException("Database error occurred"))
                .when(accountService)
                .cashWithdrawal(accountNumber, pin, amount);

            // When & Then
            mockMvc.perform(post("/api/account/withdraw")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(amountRequest)))
                    .andExpect(status().isInternalServerError())
                    .andExpect(content().string(containsString("Database error occurred")));
        }
    }  
  
    @Test
    void fundTransfer_WithValidRequest_ShouldReturnSuccessResponse() throws Exception {
        // Given
        String sourceAccountNumber = "123456";
        String targetAccountNumber = "654321";
        String pin = "1234";
        double amount = 100.00;

        try (MockedStatic<LoggedinUser> mockedStatic = mockStatic(LoggedinUser.class)) {
            mockedStatic.when(LoggedinUser::getAccountNumber).thenReturn(sourceAccountNumber);

            FundTransferRequest request = new FundTransferRequest(
                sourceAccountNumber,
                targetAccountNumber,
                amount,
                pin
            );

            // When
            mockMvc.perform(post("/api/account/fund-transfer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("Fund transferred successfully")));

            // Then
            verify(accountService).fundTransfer(
                sourceAccountNumber,
                targetAccountNumber,
                pin,
                amount
            );
        }
    }  
  
    @Test
    void fundTransfer_shouldThrowInsufficientBalanceException_whenBalanceIsInsufficient() throws Exception {
        // Given
        String sourceAccountNumber = "123456";
        String targetAccountNumber = "654321";
        String pin = "1234";
        double amount = 1000.00;

        FundTransferRequest request = new FundTransferRequest(
                sourceAccountNumber,
                targetAccountNumber,
                amount,
                pin
        );

        try (MockedStatic<LoggedinUser> mockedStatic = mockStatic(LoggedinUser.class)) {
            mockedStatic.when(LoggedinUser::getAccountNumber).thenReturn(sourceAccountNumber);

            doThrow(new InsufficientBalanceException("Insufficient balance"))
                    .when(accountService)
                    .fundTransfer(sourceAccountNumber, targetAccountNumber, pin, amount);

            // When & Then
            mockMvc.perform(post("/api/account/fund-transfer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string(ApiMessages.BALANCE_INSUFFICIENT_ERROR.getMessage()));
        }
    }  
  
    @Test
    void fundTransfer_WithInvalidPin_ThrowsInvalidPinException() throws Exception {
        // Given
        String sourceAccountNumber = "123456";
        String targetAccountNumber = "654321";
        String invalidPin = "0000";
        double amount = 100.00;

        try (MockedStatic<LoggedinUser> mockedStatic = mockStatic(LoggedinUser.class)) {
            mockedStatic.when(LoggedinUser::getAccountNumber).thenReturn(sourceAccountNumber);

            doThrow(new InvalidPinException("Invalid PIN"))
                    .when(accountService)
                    .fundTransfer(sourceAccountNumber, targetAccountNumber, invalidPin, amount);

            FundTransferRequest request = new FundTransferRequest(
                    sourceAccountNumber,
                    targetAccountNumber,
                    amount,
                    invalidPin
            );

            // When & Then
            mockMvc.perform(post("/api/account/fund-transfer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string("Invalid PIN"));
        }
    }  
  
    @Test
    void fundTransfer_shouldThrowAccountDoesNotExistException_whenTargetAccountDoesNotExist() throws Exception {
        // Given
        String sourceAccountNumber = "123456";
        String targetAccountNumber = "000000";
        String pin = "1234";
        double amount = 100.00;

        FundTransferRequest request = new FundTransferRequest(
            sourceAccountNumber, 
            targetAccountNumber,
            amount,
            pin
        );

        try (MockedStatic<LoggedinUser> mockedStatic = mockStatic(LoggedinUser.class)) {
            mockedStatic.when(LoggedinUser::getAccountNumber).thenReturn(sourceAccountNumber);
            
            doThrow(new AccountDoesNotExistException("Account does not exist"))
                .when(accountService)
                .fundTransfer(sourceAccountNumber, targetAccountNumber, pin, amount);

            // When & Then
            mockMvc.perform(post("/api/account/fund-transfer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(content().string("Account does not exist"));
        }
    }  
  
    @Test
    void fundTransfer_WhenSelfTransfer_ThrowsFundTransferException() throws Exception {
        // Given
        String accountNumber = "123456";
        String pin = "1234";
        double amount = 100.00;
        
        try (MockedStatic<LoggedinUser> mockedStatic = mockStatic(LoggedinUser.class)) {
            mockedStatic.when(LoggedinUser::getAccountNumber).thenReturn(accountNumber);
            
            FundTransferRequest request = new FundTransferRequest(
                accountNumber,
                accountNumber,
                amount,
                pin
            );
            
            doThrow(new FundTransferException("Cannot transfer to same account"))
                .when(accountService)
                .fundTransfer(accountNumber, accountNumber, pin, amount);

            // When & Then
            mockMvc.perform(post("/api/account/fund-transfer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string(containsString("Cannot transfer to same account")));
        }
    }  
  
    @Test
    void fundTransfer_shouldThrowNetworkFailureException_whenNetworkFailureOccurs() throws Exception {
        // Given
        String loggedInAccountNumber = "123456";
        String targetAccountNumber = "654321";
        double amount = 100.00;
        String pin = "1234";

        try (MockedStatic<LoggedinUser> mockedStatic = mockStatic(LoggedinUser.class)) {
            mockedStatic.when(LoggedinUser::getAccountNumber).thenReturn(loggedInAccountNumber);

            FundTransferRequest request = new FundTransferRequest(
                loggedInAccountNumber,
                targetAccountNumber,
                amount,
                pin
            );

            doThrow(new NetworkFailureException("Network failure during transfer"))
                .when(accountService)
                .fundTransfer(loggedInAccountNumber, targetAccountNumber, pin, amount);

            // When & Then
            mockMvc.perform(post("/api/account/fund-transfer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isInternalServerError())
                    .andExpect(content().string(containsString("Network failure during transfer")));
        }
    }  
  
    @Test
    void getAllTransactionsByAccountNumber_ShouldReturnTransactions() throws Exception {
        // Given
        String accountNumber = "1234567890";
        List<TransactionDTO> transactions = Arrays.asList(
            new TransactionDTO(1L, 100.0, TransactionType.CASH_DEPOSIT, new Date(), accountNumber, null),
            new TransactionDTO(2L, 50.0, TransactionType.CASH_WITHDRAWAL, new Date(), accountNumber, null)
        );
        
        try (MockedStatic<LoggedinUser> mockedStatic = mockStatic(LoggedinUser.class)) {
            mockedStatic.when(LoggedinUser::getAccountNumber).thenReturn(accountNumber);
            when(transactionService.getAllTransactionsByAccountNumber(accountNumber)).thenReturn(transactions);

            // When & Then
            mockMvc.perform(get("/api/account/transactions"))
                    .andExpect(status().isOk())
                    .andExpect(content().json(JsonUtil.toJson(transactions)));
        }
    }  
  
    @Test
    void getAllTransactionsByAccountNumber_WhenNoTransactions_ReturnsEmptyArray() throws Exception {
        // Given
        String accountNumber = "1234567890";
        when(LoggedinUser.getAccountNumber()).thenReturn(accountNumber);
        when(transactionService.getAllTransactionsByAccountNumber(accountNumber))
                .thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/api/account/transactions"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }  
  
    @Test
    void getAllTransactionsByAccountNumber_WithInvalidAccount_ReturnsNotFound() throws Exception {
        // Given
        String invalidAccountNumber = "invalid123";
        try (MockedStatic<LoggedinUser> mockedStatic = mockStatic(LoggedinUser.class)) {
            mockedStatic.when(LoggedinUser::getAccountNumber).thenReturn(invalidAccountNumber);
            
            when(transactionService.getAllTransactionsByAccountNumber(invalidAccountNumber))
                .thenThrow(new AccountDoesNotExistException("Account does not exist"));

            // When & Then
            mockMvc.perform(get("/api/account/transactions"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Account does not exist"));
        }
    }  
  
    @Test
    void getAllTransactionsByAccountNumber_WhenDatabaseError_ThenReturn500() throws Exception {
        // Given
        String accountNumber = "1234567890";
        try (MockedStatic<LoggedinUser> mockedStatic = mockStatic(LoggedinUser.class)) {
            mockedStatic.when(LoggedinUser::getAccountNumber).thenReturn(accountNumber);
            when(transactionService.getAllTransactionsByAccountNumber(accountNumber))
                    .thenThrow(new RuntimeException("Database error"));

            // When & Then
            mockMvc.perform(get("/api/account/transactions"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(content().string(containsString("Database error")));
        }
    }  
  
    @Test
    void getAllTransactionsByAccountNumber_WhenNoAccountNumber_ShouldReturnBadRequest() throws Exception {
        try (MockedStatic<LoggedinUser> mockedStatic = mockStatic(LoggedinUser.class)) {
            // Given
            mockedStatic.when(LoggedinUser::getAccountNumber).thenReturn(null);

            // When & Then
            mockMvc.perform(get("/api/account/transactions"))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string(containsString("Account number not found")));
        }
    }

    /* End section: tests */

}