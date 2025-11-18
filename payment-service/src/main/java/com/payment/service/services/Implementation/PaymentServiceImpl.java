package com.payment.service.services.Implementation;

import com.commonlib.service.IdempotencyService;
import com.payment.service.model.Payment;
import com.payment.service.repository.PaymentRepository;
import com.payment.service.services.PaymentService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository repository;
    private  final IdempotencyService idempotencyService;

    public PaymentServiceImpl(PaymentRepository repository,IdempotencyService idempotencyService) {
        this.repository = repository;
        this.idempotencyService=idempotencyService;
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

}

