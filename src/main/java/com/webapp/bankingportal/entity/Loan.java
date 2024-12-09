
package com.webapp.bankingportal.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Loan {
    
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotNull
    private double amount;
    
    @NotNull
    private double interestRate;
    
    @NotNull
    private int repaymentPeriod;
    
    @NotNull
    private double outstandingBalance;
    
    @NotEmpty
    private String description;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    private LoanStatus status;
    
    @NotNull 
    @ManyToOne 
    @JoinColumn(name = "account_id")
    private Account account;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    // @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL)
    // private List<Transaction> transactions;
    
    public Loan(Account account, double amount, String description) {
        this.account = account;
        this.amount = amount;
        this.description = description;
        this.interestRate = 5.0; // Fixed 5% annual interest rate
        this.repaymentPeriod = 12; // Fixed 1-year repayment period
        this.outstandingBalance = amount;
        this.status = LoanStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }
}