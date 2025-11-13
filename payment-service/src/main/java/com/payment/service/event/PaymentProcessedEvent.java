package com.payment.service.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Event sent back by PaymentService after processing
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentProcessedEvent {
    private String transactionId;
    private String status;
    private String description;
}
