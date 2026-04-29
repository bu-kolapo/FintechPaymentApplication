package com.transaction.service.messaging.event;

import com.transaction.service.model.Transaction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionCompletedEvent implements Serializable {
    private String transactionId;
    private String accountId;
    private String token;
    private String idempotencyKey;
    private BigDecimal amount;
    private Transaction.TransactionType type;
    private Transaction.TransactionStatus status;
    private String tenantId;
    private long timestamp;
}