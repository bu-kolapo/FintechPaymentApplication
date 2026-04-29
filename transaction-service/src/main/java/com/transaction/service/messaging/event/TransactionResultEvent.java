package com.transaction.service.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResultEvent {
    private String transactionId;
    private boolean success;
    private String reason; // optional failure reason
    private String timestamp;
}