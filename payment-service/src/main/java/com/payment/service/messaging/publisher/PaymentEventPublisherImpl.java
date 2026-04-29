package com.payment.service.messaging.publisher;

import com.payment.service.model.Payment;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class PaymentEventPublisherImpl implements PaymentEventPublisher{
    @Override
    public Mono<Void> publishPaymentCompleted(Payment payment) {
        // Publish an event to RabbitMQ, Kafka, etc.
        System.out.println("Publishing payment completed event: " + payment.getId());
        return Mono.empty();
    }

}
