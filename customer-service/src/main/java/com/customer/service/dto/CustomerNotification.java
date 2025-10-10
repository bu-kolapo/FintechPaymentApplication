package com.customer.service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

// Notification DTO
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomerNotification {
    private String customerId;
    private String customerName;
    private String eventType; // CUSTOMER_REGISTERED, CUSTOMER_UPDATED, etc.
    private Instant timestamp;
}


