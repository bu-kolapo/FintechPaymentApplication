package com.payment.service.messaging.publisher;

import com.payment.service.messaging.event.PaymentCompletedEvent;
import com.payment.service.model.Payment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebhookEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publish(Payment payment, String status) {
        try {
            rabbitTemplate.convertAndSend(
                    "payment-exchange",
                    "payment.completed",
                    PaymentCompletedEvent.builder()
                            .paymentId(payment.getId())
                            .customerId(payment.getCustomerId())
                            .tenantId(payment.getTenantId())
                            .referenceId(payment.getReferenceId())
                            .status(status)
                            .amount(payment.getAmount())
                            .currency(payment.getCurrency())
                            .sourceAccount(payment.getSourceAccount())
                            .destinationAccount(payment.getDestinationAccount())
                            .narration(payment.getNarration())
                            .initiatedAt(payment.getInitiatedAt())
                            .completedAt(payment.getCompletedAt())
                            .build()
            );
            log.info("📤 Webhook event published | paymentId={} | status={}",
                    payment.getId(), status);
        } catch (Exception e) {
            log.error("❌ Failed to publish webhook event | paymentId={} | reason={}",
                    payment.getId(), e.getMessage());
        }
    }
}