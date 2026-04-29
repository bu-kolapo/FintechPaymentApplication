
package com.payment.service.services.Implementation;

import com.commonlib.service.IdempotencyService;

import com.payment.service.dto.DebitRequest;
import com.payment.service.messaging.client.AccountClient;
import com.payment.service.dto.AccountDTO;
import com.payment.service.dto.PaymentRequest;
import com.payment.service.messaging.event.TransactionRequestEvent;
import com.payment.service.messaging.producer.PaymentProducer;
import com.payment.service.model.Payment;
import com.payment.service.messaging.publisher.PaymentEventPublisher;
import com.payment.service.repository.PaymentRepository;
import com.payment.service.services.NotificationService;
import com.payment.service.services.PaymentService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

    @Service
    @Slf4j
    public class PaymentServiceImpl implements PaymentService {

        private final PaymentRepository repository;
        private final IdempotencyService idempotencyService;
        private final RabbitTemplate rabbitTemplate;
        private final NotificationService notificationService;
        private final PaymentEventPublisher eventPublisher;
        private final PaymentProducer paymentProducer;
        private final AccountClient accountClient;


        public PaymentServiceImpl(PaymentRepository repository, IdempotencyService idempotencyService, RabbitTemplate rabbitTemplate, NotificationService notificationService,
                                  PaymentProducer paymentProducer,PaymentEventPublisher eventPublisher, AccountClient accountClient) {
            this.repository = repository;
            this.idempotencyService = idempotencyService;
            this.rabbitTemplate = rabbitTemplate;
            this.notificationService = notificationService;
            this.eventPublisher = eventPublisher;
            this.accountClient = accountClient;
            this.paymentProducer=paymentProducer;

        }

        @Override
        public Flux<Payment> getAllPayments() {
            return repository.findAll();
        }

        @Override
        public Mono<Payment> getPaymentById(String id) {
            return repository.findById(id)
                    .switchIfEmpty(Mono.error(
                            new RuntimeException("Payment not found with id: " + id)));
        }

        @Override
        @CircuitBreaker(name = "paymentServiceCB", fallbackMethod = "processPaymentFallback")
        @RateLimiter(name = "paymentServiceRL")
        public Mono<Payment> processPayment(PaymentRequest request, String idempotencyKey, String token) {

            validateRequest(request, idempotencyKey);

            return idempotencyService.exists(idempotencyKey)
                    .flatMap(exists -> {
                        if (exists) {
                            return idempotencyService.getResponse(idempotencyKey, Payment.class);
                        }
                        return resolveAccountsAndInitiate(request, idempotencyKey, token);
                    });
        }

        private void validateRequest(PaymentRequest request, String idempotencyKey) {

            if (idempotencyKey == null || idempotencyKey.isBlank()) {
                throw new RuntimeException("Idempotency-Key header is required");
            }

            if (request.getSourceAccount().equals(request.getDestinationAccount())) {
                throw new RuntimeException("Source and destination accounts cannot be the same");
            }

            if (request.getCustomerId() == null || request.getTenantId() == null) {
                throw new RuntimeException("customerId and tenantId are required");
            }
        }
        private Mono<Payment> resolveAccountsAndInitiate(PaymentRequest request,
                                                         String idempotencyKey,
                                                         String token) {

            return Mono.zip(
                    accountClient.getAccountByNumber(request.getSourceAccount(), token),
                    accountClient.getAccountByNumber(request.getDestinationAccount(), token)
            ).flatMap(tuple -> {

                AccountDTO source = tuple.getT1();
                AccountDTO destination = tuple.getT2();

                validateAccounts(request, source, destination);

                return initiatePayment(request, idempotencyKey, source, destination,token);
            });
        }
        private Mono<Payment> initiatePayment(PaymentRequest request,
                                              String idempotencyKey,
                                              AccountDTO source,
                                              AccountDTO destination, String token) {

            Payment payment = Payment.builder()
                    .sourceAccount(request.getSourceAccount())
                    .destinationAccount(request.getDestinationAccount())
                    .destinationAccountId(destination.getId())
                    .accountId(source.getId())
                    .narration("Payment Initiated")
                    .amount(request.getAmount())
                    .currency(request.getCurrency())
                    .customerId(request.getCustomerId())
                    .tenantId(request.getTenantId())
                    .type("TRANSFER")
                    .idempotencyKey(idempotencyKey)
                    .referenceId(idempotencyKey)
                    .status(Payment.PaymentStatus.PENDING)
                    .initiatedAt(Instant.now())
                    .build();

            return repository.save(payment)
                    .flatMap(saved -> {

                        // ✅ Use a strong, consistent event
                        DebitRequest debit = new DebitRequest(
                                saved.getAccountId(),
                                saved.getAmount(),
                                saved.getTenantId(),
                                saved.getIdempotencyKey() ,  // 🔥 critical
                                token
                        );

                        // ✅ USE YOUR PRODUCER (single source of truth)
                        paymentProducer.sendDebitRequest(debit);

                        log.info("📤 Debit request sent | paymentId={} | idempotencyKey={}",
                                saved.getId(), saved.getIdempotencyKey());

                        return idempotencyService.storeResponse(idempotencyKey, saved)
                                .thenReturn(saved);
                    });
        }
        private void validateAccounts(PaymentRequest request,
                                      AccountDTO source,
                                      AccountDTO destination) {

            // Ownership check
            if (!source.getCustomerId().equals(request.getCustomerId())) {
                throw new RuntimeException(
                        "Source account does not belong to customer: " + request.getCustomerId());
            }

            // Status checks
            if (!"ACTIVE".equalsIgnoreCase(source.getStatus())) {
                throw new RuntimeException("Source account is not active");
            }

            if (!"ACTIVE".equalsIgnoreCase(destination.getStatus())) {
                throw new RuntimeException("Destination account is not active");
            }

            // Currency validation
            if (!source.getCurrency().equalsIgnoreCase(destination.getCurrency())) {
                throw new RuntimeException(
                        "Currency mismatch: source=" + source.getCurrency() +
                                ", destination=" + destination.getCurrency());
            }

            // Tenant validation
            if (!source.getTenantId().equals(destination.getTenantId())) {
                throw new RuntimeException("Cross-tenant transfer not allowed");
            }
        }

        // Fallback — must mirror processPayment signature + Throwable
        private Mono<Payment> processPaymentFallback(PaymentRequest request, String idempotencyKey, String token ,Throwable ex) {
            log.error("⚡ Circuit breaker triggered for key: {}, reason: {}",
                    idempotencyKey, ex.getMessage());

            Payment fallback = Payment.builder()
                    .sourceAccount(request.getSourceAccount())
                    .destinationAccount(request.getDestinationAccount())
                    .amount(request.getAmount())
                    .currency(request.getCurrency())
                    .customerId(request.getCustomerId())
                    .tenantId(request.getTenantId())
                    .idempotencyKey(idempotencyKey)
                    .referenceId(idempotencyKey)
                    .status(Payment.PaymentStatus.FAILED)
                    .narration("Payment temporarily unavailable: " + ex.getMessage())
                    .initiatedAt(Instant.now())
                    .build();

            return Mono.just(fallback);
        }
    }
