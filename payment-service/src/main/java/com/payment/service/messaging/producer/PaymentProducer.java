package com.payment.service.messaging.producer;

import com.payment.service.dto.CreditRequest;
import com.payment.service.dto.DebitRequest;
import com.payment.service.messaging.event.TransactionRequestEvent;
import com.payment.service.messaging.queue.QueueConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentProducer {

    private final RabbitTemplate rabbitTemplate;

    public void sendDebitRequest(DebitRequest request) {
        TransactionRequestEvent event = TransactionRequestEvent.builder()
                .accountId(request.getAccountId())
                .amount(request.getAmount())
                .type("DEBIT")
                .tenantId(request.getTenantId())
                .idempotencyKey(request.getIdempotencyKey())
                .token(request.getToken())           // ✅ critical — auth for account-service
                .build();

        rabbitTemplate.convertAndSend(
                QueueConstants.EXCHANGE,
                QueueConstants.DEBIT_REQUEST,    // "debit.request"
                event);

        log.info("📤 Debit request sent | accountId={} | key={}",
                request.getAccountId(), request.getIdempotencyKey());
    }

    public void sendCreditRequest(CreditRequest request) {
        TransactionRequestEvent event = TransactionRequestEvent.builder()
                .accountId(request.getAccountId())   // destination accountId
                .amount(request.getAmount())
                .type("CREDIT")
                .tenantId(request.getTenantId())
                .idempotencyKey(request.getIdempotencyKey())
                .token(request.getToken())
                .build();

        rabbitTemplate.convertAndSend(
                QueueConstants.EXCHANGE,
                QueueConstants.CREDIT_REQUEST,
                event);

        log.info("📤 Credit request sent | accountId={} | key={}",
                request.getAccountId(), request.getIdempotencyKey());
    }
}