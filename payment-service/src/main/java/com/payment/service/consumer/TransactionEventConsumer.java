package com.payment.service.consumer;

import com.payment.service.event.TransactionCreatedEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class TransactionEventConsumer {

    @RabbitListener(queues = "payment-service-queue")
    public void handleTransactionCreated(TransactionCreatedEvent event) {
        System.out.println("📩 Received TransactionCreatedEvent for transactionId: " + event.getTransactionId());

        // Implement payment logic here
        if ("CREDIT".equals(event.getType())) {
            System.out.println("💰 Process CREDIT of " + event.getAmount() + " for account " + event.getAccountId());
        } else if ("DEBIT".equals(event.getType())) {
            System.out.println("💸 Process DEBIT of " + event.getAmount() + " for account " + event.getAccountId());
        }
    }
}