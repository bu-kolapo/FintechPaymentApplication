package com.payment.service.services.Implementation;

import com.payment.service.model.Payment;
import com.payment.service.repository.PaymentRepository;
import com.payment.service.services.PaymentService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository repository;

    public PaymentServiceImpl(PaymentRepository repository) {
        this.repository = repository;
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
    public Mono<Payment> savePayment(Payment payment) {
        return repository.save(payment);
    }
}

