package com.webapp.bankingportal.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoanRequest {
    @NotEmpty(message = "Account number cannot be empty")
    private String accountNumber;
    
    @NotNull(message = "Amount cannot be null")
    @Positive(message = "Amount must be positive")
    private double amount;
    
    @NotEmpty(message = "Description cannot be empty")
    private String description;
}