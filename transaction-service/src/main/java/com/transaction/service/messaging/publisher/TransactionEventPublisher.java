package com.transaction.service.messaging.publisher;

import com.transaction.service.dto.TransactionResponse;
import com.transaction.service.messaging.event.TransactionCompletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

//Call this publisher inside your processTransaction() method after saving the transaction.
@Component
@RequiredArgsConstructor
public class TransactionEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishTransactionCompleted(TransactionResponse response) {

        TransactionCompletedEvent event = new TransactionCompletedEvent(
                response.getId(),
                response.getAccountId(),
                response.getToken(),
                response.getIdempotencyKey(),
                response.getAmount(),
                response.getType(),
                response.getStatus(),
                response.getTenantId(),
                System.currentTimeMillis()
        );

        // ✅ ROUTE BASED ON TRANSACTION TYPE
        String routingKey;

        if (response.getType().name().equalsIgnoreCase("DEBIT")) {
            routingKey = "debit.response";
        } else {
            routingKey = "credit.response";
        }

        rabbitTemplate.convertAndSend(
                "payment-exchange",   // must match Payment service exchange
                routingKey,
                event
        );

        System.out.println("📢 Published " + routingKey +
                " for idempotencyKey=" + response.getIdempotencyKey());
    }
}