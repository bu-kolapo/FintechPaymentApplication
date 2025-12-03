package com.transaction.service.publisher;

import com.transaction.service.config.RabbitMQConfig;
import com.transaction.service.dto.TransactionResponse;
import com.transaction.service.event.TransactionCompletedEvent;
import com.transaction.service.event.TransactionCreatedEvent;
import com.transaction.service.model.Transaction;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

//Call this publisher inside your processTransaction() method after saving the transaction.
@Component
@RequiredArgsConstructor
public class TransactionEventPublisher {

    private final RabbitTemplate rabbitTemplate;

//    public TransactionEventPublisher(RabbitTemplate rabbitTemplate) {
//        this.rabbitTemplate = rabbitTemplate;
//    }

    public void publishTransactionCompleted(TransactionResponse transactionResponse) {
        try {
            TransactionCompletedEvent event = new TransactionCompletedEvent(
                    transactionResponse.getId(),
                    transactionResponse.getAccountId(),
                    transactionResponse.getAmount(),
                    transactionResponse.getType().name(),
                    transactionResponse.getStatus().name(),
                    transactionResponse.getTenantId(),
                    System.currentTimeMillis()
            );

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.TRANSACTION_EXCHANGE,
                    RabbitMQConfig.TRANSACTION_ROUTING_KEY,
                    event
            );


            System.out.println("📢 Published TRANSACTION_COMPLETED event: " + event.getTransactionId());
        } catch (Exception e) {
            System.err.println("❌ Error publishing event: " + e.getMessage());
        }
    }


}