package com.payment.service.controller;

import com.payment.service.model.ExternalCallback;
import com.payment.service.model.Payment;
import com.payment.service.repository.PaymentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/interbank/callback")
@Slf4j
public class InterbankCallbackController {

    private final PaymentRepository repository;

    public InterbankCallbackController(PaymentRepository repository) {
        this.repository = repository;
    }

    @PostMapping
    public Mono<Void> handleCallback(@RequestBody ExternalCallback payload) {

        return repository.findFirstByReferenceId(payload.getReferenceId())
                .switchIfEmpty(Mono.error(
                        new RuntimeException("Payment not found for referenceId: " + payload.getReferenceId())
                ))
                .flatMap(payment -> {

                    // store external info
                    payment.setExternalTransactionId(payload.getExternalTransactionId());
                    payment.setExternalStatusMessage(payload.getMessage());
                    payment.setNarration(payload.getMessage());      // ✅
                    payment.setProcessedAt(Instant.now());
                    if ("SUCCESS".equalsIgnoreCase(payload.getStatus())) {
                        payment.setStatus(Payment.PaymentStatus.SUCCESS);
                        payment.setCreditTransactionId(payload.getExternalTransactionId()); // ✅
                        payment.setCompletedAt(Instant.now());
                        log.info("✅ Interbank callback SUCCESS | paymentId={}", payment.getId());
                    } else {
                        payment.setStatus(Payment.PaymentStatus.FAILED);
                        payment.setCompletedAt(Instant.now());
                        log.warn("❌ Interbank callback FAILED | paymentId={}", payment.getId());
                    }
                    return repository.save(payment);
                })
                .doOnSuccess(v ->
                        System.out.println("✅ Callback processed for ref: " + payload.getReferenceId()))
                .doOnError(err ->
                        System.err.println("❌ Callback error: " + err.getMessage()))
                .then();
    }
}