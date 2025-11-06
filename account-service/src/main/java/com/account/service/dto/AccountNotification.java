package com.account.service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccountNotification {
    private String accountId;
    private String accountName;
    private String eventType; // ACCOUNT_CREATED, ACCOUNT_UPDATED, etc.
    private Instant timestamp;
}
