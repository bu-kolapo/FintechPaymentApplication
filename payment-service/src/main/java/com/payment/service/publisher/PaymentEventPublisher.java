package com.payment.service.publisher;

import com.payment.service.model.Payment;
import reactor.core.publisher.Mono;

public interface PaymentEventPublisher {
    Mono<Void> publishPaymentCompleted(Payment payment);
}
