package com.account.service.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionCompletedEvent implements Serializable {
    private String transactionId;
    private String accountId;
    private BigDecimal amount;
    private String type; // e.g. CREDIT or DEBIT
    private String status; // e.g. SUCCESS or FAILED
    private String tenantId;
    private long timestamp;
}