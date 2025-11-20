package com.payment.service.services;

import com.payment.service.model.Payment;
import reactor.core.publisher.Mono;

public interface NotificationService {
    Mono<Void> notifyUser(Payment payment);
}
