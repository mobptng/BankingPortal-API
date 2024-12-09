
package com.webapp.bankingportal.controller;

import com.webapp.bankingportal.dto.LoanRequest;
import com.webapp.bankingportal.dto.LoanResponse;
import com.webapp.bankingportal.entity.Loan;
import com.webapp.bankingportal.exception.NotImplementedException;
import com.webapp.bankingportal.service.LoanService;
import com.webapp.bankingportal.util.ApiMessages;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/loans")
@RequiredArgsConstructor
public class LoanController {
    
    private final LoanService loanService;

    @PostMapping("/apply")
    public ResponseEntity<String> applyForLoan(@RequestBody LoanRequest loanRequest) {
        Loan loan = loanService.applyForLoan(
            loanRequest.getAccountNumber(),
            loanRequest.getAmount(),
            loanRequest.getDescription()
        );
        return ResponseEntity.ok(ApiMessages.LOAN_APPLICATION_SUCCESS.getMessage());
    }

    @PostMapping("/approve/{loanId}")
    public ResponseEntity<String> approveLoan(@PathVariable Long loanId) {
        loanService.approveLoan(loanId);
        return ResponseEntity.ok(ApiMessages.LOAN_APPROVAL_SUCCESS.getMessage());
    }

    @PostMapping("/repay/{loanId}")
    public ResponseEntity<String> repayLoan(@PathVariable Long loanId, @RequestParam double amount) {
        loanService.repayLoan(loanId, amount);
        return ResponseEntity.ok(ApiMessages.LOAN_REPAYMENT_SUCCESS.getMessage());
    }

    @GetMapping("/account/{accountNumber}")
    public ResponseEntity<List<LoanResponse>> getLoansByAccount(@PathVariable String accountNumber) {
        List<Loan> loans = loanService.getLoansByAccountNumber(accountNumber);
        List<LoanResponse> loanResponses = loans.stream()
            .<LoanResponse>map(loan -> new LoanResponse(
                loan.getId(),
                loan.getAmount(),
                loan.getInterestRate(),
                loan.getRepaymentPeriod(),
                loan.getOutstandingBalance(),
                loan.getDescription(),
                loan.getStatus()
            ))
            .toList();
        return ResponseEntity.ok(loanResponses);
    }
}
