package com.payment.service.model;

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
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "payments")
public class Payment {

    @Id
    private String id;
    private String transactionId;
    private String accountId;
    private String type;
    private String sourceAccount;
    private String tenantId;
    private Instant processedAt;
    private String destinationAccount;
    private String destinationAccountId;
    private String referenceId; // idempotencyKey
    private String customerId;

    private BigDecimal amount;
    private String currency;
    private String narration;
    private PaymentStatus status;
    private ExternalStatus externalStatus;
    private String idempotencyKey;
    private String debitTransactionId;
    private String creditTransactionId;
    private String externalTransactionId;
    private String externalStatusMessage;
    private Instant initiatedAt;
    private Instant completedAt;

    public enum PaymentStatus {
        PROCESSING, SUCCESS, FAILED, PENDING,REVERSED
    }

    public enum ExternalStatus {
        SUCCESS,
        FAILED
    }
}