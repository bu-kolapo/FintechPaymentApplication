package com.payment.service.messaging.listener;

import com.payment.service.dto.CreditRequest;
import com.payment.service.messaging.event.TransactionResponse;
import com.payment.service.messaging.producer.PaymentProducer;
import com.payment.service.messaging.queue.QueueConstants;
import com.payment.service.model.Payment;
import com.payment.service.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventListener {

    private final PaymentRepository repository;
    private final PaymentProducer paymentProducer;

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

        // ✅ Strip suffix — handles both "-credit" and "-reversal"
        String baseKey = response.getIdempotencyKey()
                .replace("-credit", "")
                .replace("-reversal", "");

        boolean isReversal = response.getIdempotencyKey().endsWith("-reversal");

        repository.findByIdempotencyKey(baseKey)
                .flatMap(payment -> {

                    boolean success = "SUCCESS".equalsIgnoreCase(response.getStatus());

                    if (isReversal) {
                        // ✅ This is a reversal credit response
                        if (success) {
                            payment.setStatus(Payment.PaymentStatus.REVERSED);
                            log.info("↩️ Reversal SUCCESS | paymentId={}", payment.getId());
                        } else {
                            // Reversal failed — needs manual intervention
                            payment.setStatus(Payment.PaymentStatus.FAILED);
                            log.error("🚨 Reversal FAILED | paymentId={} — manual intervention required",
                                    payment.getId());
                        }
                        payment.setCompletedAt(Instant.now());
                        return repository.save(payment);
                    }

                    // ✅ Normal credit response
                    if (success) {
                        payment.setCreditTransactionId(response.getTransactionId());
                        payment.setStatus(Payment.PaymentStatus.SUCCESS);
                        payment.setNarration("Transfer completed successfully");
                        payment.setCompletedAt(Instant.now());
                        payment.setProcessedAt(Instant.now());
                        log.info("✅ Payment SUCCESS | paymentId={}", payment.getId());

                    } else {
                        // ✅ Credit failed — reverse the debit
                        log.warn("❌ Credit FAILED | paymentId={} | sending reversal",
                                payment.getId());

                        paymentProducer.sendCreditRequest(
                                new CreditRequest(
                                        payment.getAccountId(),    // ✅ refund to SOURCE account
                                        payment.getAmount(),
                                        payment.getTenantId(),
                                        payment.getIdempotencyKey() + "-reversal",
                                        response.getToken()
                                )
                        );

                        payment.setStatus(Payment.PaymentStatus.REVERSED);
                        log.info("↩️ Reversal sent | paymentId={}", payment.getId());
                    }

                    return repository.save(payment);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("⚠️ No payment found for credit baseKey: {}", baseKey);
                    return Mono.empty();
                }))
                .doOnError(err ->
                        log.error("❌ Error in credit response handler: {}", err.getMessage()))
                .subscribe();
    }
}