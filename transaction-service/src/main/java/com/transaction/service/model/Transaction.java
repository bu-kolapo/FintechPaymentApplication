package com.transaction.service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "transactions")
public class Transaction {

    @Id
    private String id;
    private String tenantId;
    private String paymentId;
    private String accountId;

    private TransactionType type;
    private BigDecimal amount;
    private TransactionStatus status;
    private String description;
    private Instant processedAt;
    private Instant createdAt;

    // getters and setters


    public enum TransactionType {
        DEBIT,
        CREDIT
    }

    public enum TransactionStatus {
        PENDING,
        CONFIRMED,
        FAILED
    }
}
