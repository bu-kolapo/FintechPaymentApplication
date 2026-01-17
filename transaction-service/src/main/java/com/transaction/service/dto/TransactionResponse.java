package com.transaction.service.dto;

import com.transaction.service.model.Transaction;
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
public  class TransactionResponse {
    private String id;
    private String tenantId;
    private String accountId;
    private String idempotencyKey;
    private String description;
    private String message;

    private Transaction.TransactionType type;
    private BigDecimal amount;
    private Transaction.TransactionStatus status;
    private Instant processedAt;
    private Instant createdAt;

}