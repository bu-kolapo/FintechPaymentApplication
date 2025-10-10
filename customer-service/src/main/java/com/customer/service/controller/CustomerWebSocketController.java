package com.customer.service.controller;

import com.customer.service.dto.CustomerNotification;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.Instant;

// WebSocket Controller for bidirectional communication
@Controller
public class CustomerWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;

    public CustomerWebSocketController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    // Clients can subscribe to this to receive updates
    @MessageMapping("/customer/subscribe")
    @SendTo("/topic/customers")
    public CustomerNotification subscribeToCustomerUpdates(String tenantId) {
        return new CustomerNotification(
                null,
                "Subscribed to customer updates",
                "SUBSCRIPTION_CONFIRMED",
                Instant.now()
        );
    }
}