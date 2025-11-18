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
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "paymentdb")
public class Payment {
    @Id
    private String id;
    private String tenantId;
    private String customerId;
    private String accountId;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String reference;
    private String type;
    private String idempotencyKey;
    private String transactionId;

    private Instant createdAt=Instant.now();
    private Instant processedAt=Instant.now();
    private Instant updatedAt;

    // getters and setters


   public enum PaymentStatus {
    PENDING,
    COMPLETED,
    FAILED
   }
}