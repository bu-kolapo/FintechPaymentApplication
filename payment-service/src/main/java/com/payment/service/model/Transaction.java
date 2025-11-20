package com.payment.service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "transactiondb")
@CompoundIndexes({
        @CompoundIndex(name = "unique_idempotency_key", def = "{'idempotencyKey': 1}", unique = true)
})
public class Transaction {

    @Id
    private String id;
    private String tenantId;
    private String accountId;
    private String idempotencyKey;
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
        SUCCESS,
        PENDING,
        CONFIRMED,
        FAILED
    }
}
