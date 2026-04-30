package com.payment.service.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TransactionRequestEvent {
    private String accountId;
    private BigDecimal amount;
    private String type; // DEBIT / CREDIT
    private String tenantId;
    private String token;
    private String idempotencyKey;

}