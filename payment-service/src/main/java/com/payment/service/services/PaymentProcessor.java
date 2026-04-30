package com.payment.service.services;

import com.payment.service.dto.AccountDTO;
import com.payment.service.dto.PaymentRequest;
import com.payment.service.model.Payment;
import reactor.core.publisher.Mono;

public interface PaymentProcessor {

    Mono<Payment> processInternalOrInterBankPayment(PaymentRequest request, String idempotencyKey, String token, AccountDTO source, AccountDTO destination);
}
