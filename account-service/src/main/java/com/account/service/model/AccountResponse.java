package com.account.service.model;

import com.account.service.dto.AccountRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountResponse {

    private String id;
    private String tenantId;
    private String customerId;
    private String accountNumber;
    private String currency;
    private BigDecimal balance;
    private Account.AccountStatus status;
    private Instant createdAt;
    private Instant updatedAt;


    public AccountResponse(Object o, String message) {
    }
}