package com.account.service.client;

import com.account.service.dto.CustomerDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class CustomerClient {

    private final WebClient webClient;

    public CustomerClient(@Value("${customer.service.url}") String baseUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public Mono<CustomerDTO> getCustomerById(String customerId, String token) {
        return webClient.get()
                .uri("/api/v1/customer/{id}", customerId)
                .header(HttpHeaders.AUTHORIZATION, token) // ✅ forward JWT exactly like your other method
                .retrieve()
                .onStatus(status -> status.value() == 404, response ->
                        Mono.error(new RuntimeException("Customer not found: " + customerId)))
                .onStatus(status -> status.is5xxServerError(), response ->
                        Mono.error(new RuntimeException("Customer service error")))
                .bodyToMono(CustomerDTO.class)
                .doOnSuccess(c ->
                        log.info("✅ Fetched customer {} → {}", customerId, c.getFullName()))
                .doOnError(err ->
                        log.error("❌ Failed to fetch customer {}: {}", customerId, err.getMessage()));
    }
}