package com.payment.service.controller;

import com.payment.service.messaging.publisher.WebhookEventPublisher;
import com.payment.service.model.ExternalCallback;
import com.payment.service.model.Payment;
import com.payment.service.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/interbank/callback")
@RequiredArgsConstructor
@Slf4j
public class InterbankCallbackController {

    private final PaymentRepository repository;
    private final WebhookEventPublisher webhookEventPublisher; // ✅ injected

    @PostMapping
    public Mono<ResponseEntity<String>> handleCallback(
            @RequestBody ExternalCallback payload) {

        log.info("📥 Interbank callback received | referenceId={} | status={}",
                payload.getReferenceId(), payload.getStatus());

        return repository.findFirstByReferenceId(payload.getReferenceId())
                .switchIfEmpty(Mono.error(
                        new RuntimeException("Payment not found for referenceId: "
                                + payload.getReferenceId())
                ))
                .flatMap(payment -> updatePayment(payment, payload))
                .flatMap(saved -> {

                    // ✅ Publish webhook event after save
                    webhookEventPublisher.publish(saved, saved.getStatus().name());

                    log.info("✅ Callback processed | paymentId={} | status={}",
                            saved.getId(), saved.getStatus());

                    return Mono.just(ResponseEntity.ok(
                            "Callback processed for referenceId: "
                                    + payload.getReferenceId()));
                })
                .onErrorResume(err -> {
                    log.error("❌ Callback error | referenceId={} | reason={}",
                            payload.getReferenceId(), err.getMessage());

                    return Mono.just(ResponseEntity
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("Callback failed: " + err.getMessage()));
                });
    }

    private Mono<Payment> updatePayment(Payment payment, ExternalCallback payload) {

        // Set external bank fields
        payment.setExternalTransactionId(payload.getExternalTransactionId());
        payment.setExternalStatusMessage(payload.getMessage());
        payment.setNarration(payload.getMessage());
        payment.setProcessedAt(Instant.now());

        if ("SUCCESS".equalsIgnoreCase(payload.getStatus())) {
            payment.setStatus(Payment.PaymentStatus.SUCCESS);
            payment.setCreditTransactionId(payload.getExternalTransactionId());
            payment.setCompletedAt(Instant.now());
            log.info("✅ Interbank SUCCESS | paymentId={} | externalTxnId={}",
                    payment.getId(), payload.getExternalTransactionId());
        } else {
            payment.setStatus(Payment.PaymentStatus.FAILED);
            payment.setCompletedAt(Instant.now());
            log.warn("❌ Interbank FAILED | paymentId={} | reason={}",
                    payment.getId(), payload.getMessage());
        }

        return repository.save(payment);
    }
}