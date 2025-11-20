package com.payment.service.services.Implementation;

import com.commonlib.service.IdempotencyService;
import com.payment.service.dto.PaymentRequest;
import com.payment.service.model.Payment;
import com.payment.service.model.Transaction;
import com.payment.service.publisher.PaymentEventPublisher;
import com.payment.service.repository.PaymentRepository;
import com.payment.service.services.NotificationService;
import com.payment.service.services.PaymentService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository repository;
    private  final IdempotencyService idempotencyService;
    private final RabbitTemplate rabbitTemplate;
    private final NotificationService notificationService;
    private final PaymentEventPublisher eventPublisher;

    public PaymentServiceImpl(PaymentRepository repository,IdempotencyService idempotencyService,
                              RabbitTemplate rabbitTemplate,NotificationService notificationService,PaymentEventPublisher eventPublisher) {
        this.repository = repository;
        this.idempotencyService=idempotencyService;
        this.rabbitTemplate=rabbitTemplate;
        this.notificationService=notificationService;
        this.eventPublisher=eventPublisher;
    }
    @Override
    public Flux<Payment> getAllPayments() {
        return repository.findAll();
    }
   @Override
    public Mono<Payment> getPaymentById(String id) {
        return repository.findById(id)
                .switchIfEmpty(Mono.error(new RuntimeException("Payment not found")));
    }
    @Override
    @CircuitBreaker(name = "paymentServiceCB", fallbackMethod = "paymentFallback")
    @RateLimiter(name = "paymentServiceRL")
    public Mono<Payment> savePayment(Payment payment) {
        String key = payment.getIdempotencyKey();
        if (key == null || key.isBlank()) {
            return Mono.error(new RuntimeException("❌ idempotencyKey is required"));
        }

        // 1️⃣ Check Redis
        return idempotencyService.exists(key)
                .flatMap(exists -> {
                    if (exists) {
                        return idempotencyService.getResponse(key, Payment.class);
                    }

                    // 2️⃣ Save payment normally
                    return repository.save(payment)
                            // 3️⃣ Store result in Redis
                            .flatMap(savedPayment -> idempotencyService.storeResponse(key, savedPayment)
                                    .thenReturn(savedPayment));
                });
    }

    @Override
    public Mono<Payment> processPayment(PaymentRequest paymentRequest) {

        String key = paymentRequest.getIdempotencyKey();

        if (key == null || key.isBlank()) {
            return Mono.error(new RuntimeException("❌ idempotencyKey is required"));
        }

        // 1️⃣ Check idempotency
        return idempotencyService.exists(key).flatMap(exists -> {
            if (exists) {
                return idempotencyService.getResponse(key, Payment.class);
            }

            // 2️⃣ Publish debit request to RabbitMQ
            return sendDebitRequest(paymentRequest)
                    .flatMap(debitTxn -> sendCreditRequest(paymentRequest)
                            .flatMap(creditTxn -> savePaymentRecord(paymentRequest, debitTxn, creditTxn))
                    );
        });
    }

    @Override
    public Mono<Payment> paymentFallback(PaymentRequest request, Throwable ex) {
        return Mono.error(new RuntimeException("Payment failed: " + ex.getMessage()));
    }

    private Mono<Transaction> sendDebitRequest(PaymentRequest request) {
        Transaction debitTxn = new Transaction();
        debitTxn.setAccountId(request.getSourceAccount());
        debitTxn.setAmount(request.getAmount());
        debitTxn.setType(Transaction.TransactionType.DEBIT);
        debitTxn.setIdempotencyKey(request.getIdempotencyKey() + "-debit");

        // Publish debit request to RabbitMQ
        rabbitTemplate.convertAndSend("transaction.exchange", "debit.request", debitTxn);

        // Here you would listen for a debit-completed event asynchronously
        return Mono.create(sink -> {
            // Example placeholder: when TransactionService publishes debit completed, complete the sink
            // sink.success(debitTxn);
        });
    }

    private Mono<Transaction> sendCreditRequest(PaymentRequest request) {
        Transaction creditTxn = new Transaction();
        creditTxn.setAccountId(request.getDestinationAccount());
        creditTxn.setAmount(request.getAmount());
        creditTxn.setType(Transaction.TransactionType.CREDIT);
        creditTxn.setIdempotencyKey(request.getIdempotencyKey() + "-credit");

        // Publish credit request to RabbitMQ
        rabbitTemplate.convertAndSend("transaction.exchange", "credit.request", creditTxn);

        // Listen for credit-completed event asynchronously
        return Mono.create(sink -> {
            // Placeholder: when TransactionService publishes credit completed, complete the sink
            // sink.success(creditTxn);
        });
    }

    private Mono<Payment> savePaymentRecord(PaymentRequest req, Transaction debitTxn, Transaction creditTxn) {
        Payment payment = new Payment();
        payment.setSourceAccount(req.getSourceAccount());
        payment.setDestinationAccount(req.getDestinationAccount());
        payment.setAmount(req.getAmount());
        payment.setDebitTransactionId(debitTxn.getId());
        payment.setCreditTransactionId(creditTxn.getId());
        payment.setStatus("SUCCESS");
        payment.setProcessedAt(Instant.now());

        return repository.save(payment)
                .flatMap(saved ->
                        eventPublisher.publishPaymentCompleted(saved)
                                .then(notificationService.notifyUser(saved))
                                .thenReturn(saved)
                );
    }


}

