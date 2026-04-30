package com.payment.service.model;

import lombok.Data;

import java.time.Instant;

@Data
public class ExternalCallback {

    // 🔑 MUST match your payment.referenceId
    private String referenceId;

    // SUCCESS / FAILED
    private String status;

    // External system transaction ID (very important)
    private String externalTransactionId;

    // Optional failure reason
    private String message;

    // Optional metadata
    private String bankCode;
    private String beneficiaryAccount;

    // Timestamp from external system (optional but useful)
    private Instant processedAt;
}
