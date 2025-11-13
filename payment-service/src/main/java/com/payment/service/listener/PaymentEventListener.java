package com.payment.service.listener;


import com.payment.service.event.TransactionCompletedEvent;
import com.payment.service.model.Payment;
import com.payment.service.repository.PaymentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import java.time.Instant;

@Service
@Slf4j
public class PaymentEventListener {

    private final PaymentRepository paymentRepository;

    public PaymentEventListener(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    // 👂 Listen to messages published by the Transaction Service
    @RabbitListener(queues = "payment-service-queue")
    public void handleTransactionEvent(TransactionCompletedEvent event) {
        log.info("📥 Received TransactionCompletedEvent: {}", event);

        Payment payment = Payment.builder()
                .transactionId(event.getTransactionId())
                .accountId(event.getAccountId())
                .amount(event.getAmount())
                .type(event.getType())
                .status(event.getStatus())
                .tenantId(event.getTenantId())
                .processedAt(Instant.ofEpochMilli(event.getTimestamp()))
                .build();

        paymentRepository.save(payment)
                .doOnSuccess(saved -> log.info("✅ Payment saved for transaction ID: {}", saved.getTransactionId()))
                .doOnError(err -> log.error("❌ Failed to save payment: {}", err.getMessage()))
                .subscribe();
    }
}