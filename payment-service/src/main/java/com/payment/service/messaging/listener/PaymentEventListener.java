package com.payment.service.messaging.listener;

import com.payment.service.dto.CreditRequest;
import com.payment.service.messaging.event.PaymentCompletedEvent;
import com.payment.service.messaging.event.TransactionResponse;
import com.payment.service.messaging.producer.PaymentProducer;
import com.payment.service.messaging.queue.QueueConstants;
import com.payment.service.model.Payment;
import com.payment.service.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventListener {

    private final PaymentRepository repository;
    private final PaymentProducer paymentProducer;

    private final RabbitTemplate rabbitTemplate;


    // =========================
    // DEBIT RESPONSE LISTENER
    // =========================
    @RabbitListener(queues = QueueConstants.DEBIT_RESPONSE_QUEUE)
    public void handleDebitResponse(TransactionResponse response) {
        log.info("📥 Debit response | key={} | status={}",
                response.getIdempotencyKey(), response.getStatus());

        repository.findByIdempotencyKey(response.getIdempotencyKey())
                .flatMap(payment -> {

                    boolean success = "SUCCESS".equalsIgnoreCase(response.getStatus()); // ✅ safe string compare

                    if (success) {
                        payment.setDebitTransactionId(response.getTransactionId());
                        payment.setTransactionId(response.getTransactionId());
                        payment.setProcessedAt(Instant.now());

                        // ✅ Only send internal credit for TRANSFER type
                        // INTERBANK_TRANSFER credit is handled by external bank callback
                        if ("TRANSFER".equalsIgnoreCase(payment.getType())) {

                            paymentProducer.sendCreditRequest(
                                    new CreditRequest(
                                            payment.getDestinationAccountId(), // ✅ destination first
                                            payment.getAmount(),
                                            payment.getTenantId(),
                                            payment.getIdempotencyKey() + "-credit",
                                            response.getToken()              // ✅ token from response
                                    )
                            );

                            log.info("📤 Credit request sent | paymentId={} | destAccountId={}",
                                    payment.getId(), payment.getDestinationAccountId());
                        } else {
                            // INTERBANK — debit done, waiting for external bank callback
                            log.info("🏦 INTERBANK debit done | paymentId={} | awaiting callback",
                                    payment.getId());
                        }

                        payment.setStatus(Payment.PaymentStatus.PROCESSING);

                    } else {
                        payment.setStatus(Payment.PaymentStatus.FAILED);
                        log.warn("❌ Debit FAILED | key={} | paymentId={}",
                                response.getIdempotencyKey(), payment.getId());
                    }

                    return repository.save(payment);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("⚠️ No payment found for debit key: {}",
                            response.getIdempotencyKey());
                    return Mono.empty();
                }))
                .doOnError(err ->
                        log.error("❌ Error in debit response handler: {}", err.getMessage()))
                .subscribe();
    }

    // =========================
    // CREDIT RESPONSE LISTENER
    // =========================
    @RabbitListener(queues = QueueConstants.CREDIT_RESPONSE_QUEUE)
    public void handleCreditResponse(TransactionResponse response) {
        log.info("📥 Credit response | key={} | status={}",
                response.getIdempotencyKey(), response.getStatus());

        String baseKey = response.getIdempotencyKey()
                .replace("-credit", "")
                .replace("-reversal", "");

        boolean isReversal = response.getIdempotencyKey().endsWith("-reversal");

        repository.findByIdempotencyKey(baseKey)
                .flatMap(payment -> {

                    boolean success = "SUCCESS".equalsIgnoreCase(response.getStatus());

                    if (isReversal) {
                        if (success) {
                            payment.setStatus(Payment.PaymentStatus.REVERSED);
                            payment.setCompletedAt(Instant.now());
                            log.info("↩️ Reversal SUCCESS | paymentId={}", payment.getId());

                            // ✅ Notify webhook — payment was reversed
                            publishWebhookEvent(payment, "REVERSED");

                        } else {
                            payment.setStatus(Payment.PaymentStatus.FAILED);
                            payment.setCompletedAt(Instant.now());
                            log.error("🚨 Reversal FAILED | paymentId={} — manual intervention required",
                                    payment.getId());

                            // ✅ Notify webhook — reversal failed
                            publishWebhookEvent(payment, "FAILED");
                        }
                        return repository.save(payment);
                    }

                    // Normal credit response
                    if (success) {
                        payment.setCreditTransactionId(response.getTransactionId());
                        payment.setTransactionId(response.getTransactionId());
                        payment.setStatus(Payment.PaymentStatus.SUCCESS);
                        payment.setNarration("Transfer completed successfully");
                        payment.setCompletedAt(Instant.now());
                        payment.setProcessedAt(Instant.now());
                        log.info("✅ Payment SUCCESS | paymentId={}", payment.getId());

                        return repository.save(payment)
                                .doOnSuccess(saved -> {
                                    // ✅ Publish webhook event AFTER save confirms
                                    publishWebhookEvent(saved, "SUCCESS");
                                });

                    } else {
                        log.warn("❌ Credit FAILED | paymentId={} | sending reversal",
                                payment.getId());

                        paymentProducer.sendCreditRequest(
                                new CreditRequest(
                                        payment.getAccountId(),
                                        payment.getAmount(),
                                        payment.getTenantId(),
                                        payment.getIdempotencyKey() + "-reversal",
                                        response.getToken()
                                )
                        );

                        payment.setStatus(Payment.PaymentStatus.REVERSED);
                        log.info("↩️ Reversal sent | paymentId={}", payment.getId());

                        return repository.save(payment);
                    }
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("⚠️ No payment found for credit baseKey: {}", baseKey);
                    return Mono.empty();
                }))
                .doOnError(err ->
                        log.error("❌ Error in credit response handler: {}", err.getMessage()))
                .subscribe();
    }

    // ✅ Extracted helper — publishes to webhook-service via RabbitMQ,webhook event published after SUCCESS:
    private void publishWebhookEvent(Payment payment, String status) {
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
            // ✅ Never let webhook publishing crash the payment flow
            log.error("❌ Failed to publish webhook event | paymentId={} | reason={}",
                    payment.getId(), e.getMessage());
        }
    }
}