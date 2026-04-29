package com.account.service.dto;

import com.account.service.dto.AccountRequest;
import com.account.service.model.Account;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class AccountResponse {

    private String id;
    private String tenantId;
    private String customerId;
    private String customerName;
    private String accountNumber;
    private String currency;
    private String message;
    private BigDecimal openingBalance;
    private BigDecimal balance;
    private Account.AccountStatus status;
    private Instant createdAt;
    private Instant updatedAt;



}