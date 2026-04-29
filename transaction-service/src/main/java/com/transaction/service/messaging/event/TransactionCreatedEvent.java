package com.transaction.service.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
@Data
@NoArgsConstructor
@AllArgsConstructor
// Event sent by TransactionService to PaymentService
public class TransactionCreatedEvent {
    private  String transactionId;
    private String accountId;
    private BigDecimal amount;
    private String type;
    private String idempotencyKey;
}
