package com.transaction.service.messaging.event;

import com.transaction.service.model.Transaction;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionRequestEvent {
    private String accountId;
    private BigDecimal amount;
    private String tenantId;
    private Transaction.TransactionType type;
    private Transaction.TransactionStatus status;
    private String idempotencyKey;
    private String token;
}