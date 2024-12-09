package com.webapp.bankingportal.dto;

import com.webapp.bankingportal.entity.LoanStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoanResponse {
    private Long id;
    private double amount;
    private double interestRate;
    private int repaymentPeriod;
    private double outstandingBalance;
    private String description;
    private LoanStatus status;
}