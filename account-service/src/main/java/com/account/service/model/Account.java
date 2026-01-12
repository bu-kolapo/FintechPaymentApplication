package com.account.service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

@Document(collection = "account_db")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Account {

    @Id
    private String id;
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

