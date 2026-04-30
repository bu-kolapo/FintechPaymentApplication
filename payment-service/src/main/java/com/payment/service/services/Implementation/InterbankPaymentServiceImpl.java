package com.payment.service.services.Implementation;

import com.commonlib.service.IdempotencyService;
import com.payment.service.dto.AccountDTO;
import com.payment.service.dto.DebitRequest;
import com.payment.service.dto.PaymentRequest;
import com.payment.service.messaging.client.ExternalBankClient;
import com.payment.service.messaging.producer.PaymentProducer;
import com.payment.service.model.Payment;
import com.payment.service.repository.PaymentRepository;
import com.payment.service.services.PaymentProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Service("interbankPaymentProcessor")
@Slf4j
public class InterbankPaymentServiceImpl implements PaymentProcessor {

    private final PaymentRepository repository;
    private final PaymentProducer paymentProducer;
    private final ExternalBankClient externalBankClient;
    private final IdempotencyService idempotencyService;

    public InterbankPaymentServiceImpl(PaymentRepository repository,
                                       PaymentProducer paymentProducer,
                                       ExternalBankClient externalBankClient,
                                       IdempotencyService idempotencyService) {
        this.repository = repository;
        this.paymentProducer = paymentProducer;
        this.externalBankClient = externalBankClient;
        this.idempotencyService = idempotencyService;
    }

    @Override
    public Mono<Payment> processInternalOrInterBankPayment(PaymentRequest request,
                                                           String idempotencyKey,
                                                           String token,
                                                           AccountDTO source,
                                                           AccountDTO destination) {
        Payment payment = Payment.builder()
                .sourceAccount(request.getSourceAccount())
                .destinationAccount(request.getDestinationAccount()) // ✅ account number
                .destinationAccountId(destination.getId())           // ✅ internal ID
                .accountId(source.getId())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .narration(request.getNarration())
                .customerId(request.getCustomerId())
                .tenantId(request.getTenantId())
                .type("INTERBANK_TRANSFER")
                .idempotencyKey(idempotencyKey)
                .referenceId(idempotencyKey)
                .status(Payment.PaymentStatus.PENDING)
                .initiatedAt(Instant.now())
                .build();

        return repository.save(payment)
                .flatMap(saved -> {

                    // Step 1 — debit source account locally
                    paymentProducer.sendDebitRequest(
                            new DebitRequest(
                                    saved.getAccountId(),
                                    saved.getAmount(),
                                    saved.getTenantId(),
                                    saved.getIdempotencyKey(),
                                    token
                            )
                    );

                    log.info("📤 [INTERBANK] Debit sent | paymentId={} | accountId={} | key={}",
                            saved.getId(), saved.getAccountId(), saved.getIdempotencyKey());

                    // Step 2 — notify external bank
                    // External bank calls back POST /api/v1/interbank/callback on completion
                    return externalBankClient.initiateTransfer(saved)
                            .flatMap(response -> {
                                log.info("🏦 External bank acknowledged | paymentId={}",
                                        saved.getId());
                                saved.setStatus(Payment.PaymentStatus.PROCESSING);
                                saved.setNarration("Awaiting external bank confirmation");
                                return repository.save(saved);
                            })
                            .onErrorResume(err -> {
                                // ✅ external bank unreachable — mark failed, don't throw
                                log.error("❌ External bank call failed: {}", err.getMessage());
                                saved.setStatus(Payment.PaymentStatus.FAILED);
                                saved.setNarration("External bank unreachable: "
                                        + err.getMessage());
                                return repository.save(saved);
                            })
                            .flatMap(updated ->
                                    idempotencyService.storeResponse(idempotencyKey, updated)
                                            .thenReturn(updated));
                });
    }
}