package com.account.service.dto;

import com.account.service.model.Account;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AccountRequest {

    private String tenantId;
    private String customerId;
    private String currency;
    private String accountNumber;
    private BigDecimal openingBalance;

    // getters and setters
// ❌ Removed: private String idempotencyKey;
    // ❌ Removed: private String accountId (set by DB, not client)
    // ❌ Removed: private String accountNumber (generated server-side)
    // ❌ Removed: private AccountStatus status (always ACTIVE on creation)
    // ❌ Removed: private Instant createdAt/updatedAt (set server-side)

    public enum AccountStatus {
        ACTIVE,
        INACTIVE,
        CLOSED
    }
}
