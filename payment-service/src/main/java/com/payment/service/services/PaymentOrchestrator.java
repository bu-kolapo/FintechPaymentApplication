package com.payment.service.services;

import com.payment.service.dto.AccountDTO;
import com.payment.service.dto.PaymentRequest;
import com.payment.service.messaging.client.AccountClient;
import com.payment.service.model.Payment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class PaymentOrchestrator {

    private final PaymentProcessor internalProcessor;
    private final PaymentProcessor interbankProcessor;
    private final AccountClient accountClient;

    public PaymentOrchestrator(
            @Qualifier("internalPaymentProcessor") PaymentProcessor internalProcessor,
            @Qualifier("interbankPaymentProcessor") PaymentProcessor interbankProcessor,
            AccountClient accountClient) {
        this.internalProcessor = internalProcessor;
        this.interbankProcessor = interbankProcessor;
        this.accountClient = accountClient;
    }

    public Mono<Payment> process(PaymentRequest request, String key, String token) {

        return Mono.zip(
                accountClient.getAccountByNumber(request.getSourceAccount(), token),
                accountClient.getAccountByNumber(request.getDestinationAccount(), token)
        ).flatMap(tuple -> {
            AccountDTO source = tuple.getT1();
            AccountDTO destination = tuple.getT2();

            boolean crossTenant = !source.getTenantId().equals(destination.getTenantId());

            log.info("🔀 Route: {} | sourceTenant={} | destTenant={}",
                    crossTenant ? "INTERBANK" : "INTERNAL",
                    source.getTenantId(),
                    destination.getTenantId());

            if (crossTenant) {
                // ✅ pass resolved accounts
                return interbankProcessor.processInternalOrInterBankPayment(
                        request, key, token, source, destination);
            }

            // ✅ pass resolved accounts
            return internalProcessor.processInternalOrInterBankPayment(
                    request, key, token, source, destination);
        });
    }
}