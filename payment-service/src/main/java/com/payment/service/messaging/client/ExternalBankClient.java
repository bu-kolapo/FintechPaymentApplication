package com.payment.service.messaging.client;

import com.payment.service.model.Payment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

//@Component
//public class ExternalBankClient {
//
//    private final WebClient webClient;
//
//    public ExternalBankClient(WebClient.Builder builder) {
//        this.webClient = builder.baseUrl("https://external-bank/api").build();
//    }
//
//    public Mono<String> initiateTransfer(Payment payment) {
//        return webClient.post()
//                .uri("/transfers")
//                .bodyValue(payment)
//                .retrieve()
//                .bodyToMono(String.class);
//    }
//}

@Component
@Slf4j
public class ExternalBankClient {

    private final WebClient webClient;

    public ExternalBankClient(WebClient.Builder builder) {
        this.webClient = builder.baseUrl("https://external-bank/api").build();
    }

    public Mono<String> initiateTransfer(Payment payment) {
        // TODO: replace with real external bank URL
        // For testing — simulate a successful external bank response
        log.info("🏦 [MOCK] Simulating external bank transfer for paymentId={}",
                payment.getId());
        return Mono.just("ACCEPTED");
    }
}
