package com.account.service.dto;

import com.account.service.model.Account;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccountRequest {

    private String accountId;
    private String tenantId;
    private String customerId;

    private String accountNumber;
    private String currency;
    private BigDecimal balance;
    private AccountStatus status;

    private Instant createdAt;
    private Instant updatedAt;

    // getters and setters


    public enum AccountStatus {
        ACTIVE,
        INACTIVE,
        CLOSED
    }
}
