package com.payment.service.services;

import com.payment.service.model.Payment;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PaymentService {
    Flux<Payment> getAllPayments();
    Mono<Payment> getPaymentById(String id);
    Mono<Payment> savePayment(Payment payment);
}
