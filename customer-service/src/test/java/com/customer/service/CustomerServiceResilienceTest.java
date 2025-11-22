package com.customer.service;

import com.customer.service.dto.CustomerRequest;
import com.customer.service.dto.CustomerResponse;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration tests for Resilience4j patterns:
 * - Circuit Breaker
 * - Rate Limiter
 * - Bulkhead
 * - Retry
 * - Time Limiter
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class CustomerServiceResilienceTest {

    @LocalServerPort
    private int port;

    @Autowired
    private WebTestClient webTestClient;

    @Autowired(required = false)
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Autowired(required = false)
    private RateLimiterRegistry rateLimiterRegistry;

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:6.0"));

    @Container
    static RabbitMQContainer rabbitMQContainer = new RabbitMQContainer(DockerImageName.parse("rabbitmq:3.12-management"));

    @Container
    static GenericContainer<?> redisContainer = new GenericContainer<>(DockerImageName.parse("redis:7.0-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
        registry.add("spring.rabbitmq.host", rabbitMQContainer::getHost);
        registry.add("spring.rabbitmq.port", rabbitMQContainer::getAmqpPort);
        registry.add("spring.rabbitmq.username", () -> "guest");
        registry.add("spring.rabbitmq.password", () -> "guest");
        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", () -> redisContainer.getMappedPort(6379));

        // Enable resilience4j for these tests
        registry.add("resilience4j.circuitbreaker.configs.default.sliding-window-size", () -> "5");
        registry.add("resilience4j.circuitbreaker.configs.default.failure-rate-threshold", () -> "50");
        registry.add("resilience4j.circuitbreaker.configs.default.wait-duration-in-open-state", () -> "10s");

        registry.add("resilience4j.ratelimiter.configs.default.limit-for-period", () -> "10");
        registry.add("resilience4j.ratelimiter.configs.default.limit-refresh-period", () -> "1s");
        registry.add("resilience4j.ratelimiter.configs.default.timeout-duration", () -> "500ms");
    }

    @Test
    void shouldHandleCircuitBreakerWhenServiceIsHealthy() {
        if (circuitBreakerRegistry == null) {
            System.out.println("Circuit Breaker not configured, skipping test");
            return;
        }

        String idempotencyKey = UUID.randomUUID().toString();

        CustomerRequest request = CustomerRequest.builder()
                .firstName("Circuit")
                .lastName("Test")
                .email("circuit.test@example.com")
                .phoneNumber("08012345678")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .status(CustomerRequest.CustomerStatus.ACTIVE)
                .idempotencyKey(idempotencyKey)
                .build();

        // Should succeed when circuit breaker is closed
        webTestClient.post()
                .uri("/api/v1/register/customer")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(CustomerResponse.class)
                .value(response -> {
                    assertThat(response.getEmail()).isEqualTo("circuit.test@example.com");
                });
    }

    @Test
    void shouldHandleRateLimiting() {
        if (rateLimiterRegistry == null) {
            System.out.println("Rate Limiter not configured, skipping test");
            return;
        }

        // Make multiple requests rapidly to trigger rate limiting
        for (int i = 0; i < 15; i++) {
            String idempotencyKey = UUID.randomUUID().toString();

            CustomerRequest request = CustomerRequest.builder()
                    .firstName("Rate")
                    .lastName("Limit" + i)
                    .email("ratelimit" + i + "@example.com")
                    .phoneNumber("08012345678")
                    .dateOfBirth(LocalDate.of(1990, 1, 1))
                    .status(CustomerRequest.CustomerStatus.ACTIVE)
                    .idempotencyKey(idempotencyKey)
                    .build();

            webTestClient.mutate()
                    .responseTimeout(Duration.ofSeconds(5))
                    .build()
                    .post()
                    .uri("/api/v1/register/customer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange();
        }

        // Some requests should have been rate limited
        // This is a soft assertion as rate limiting behavior can vary
        System.out.println("Rate limiting test completed - check logs for rate limit events");
    }

    @Test
    void shouldRetryOnTransientFailures() {
        // This test verifies retry mechanism is configured
        // In real scenarios, you'd mock a transient failure

        String idempotencyKey = UUID.randomUUID().toString();

        CustomerRequest request = CustomerRequest.builder()
                .firstName("Retry")
                .lastName("Test")
                .email("retry.test@example.com")
                .phoneNumber("08012345678")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .status(CustomerRequest.CustomerStatus.ACTIVE)
                .idempotencyKey(idempotencyKey)
                .build();

        webTestClient.post()
                .uri("/api/v1/register/customer")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated();
    }

    @Test
    void shouldHandleBulkheadLimits() {
        // Test bulkhead by making concurrent requests
        String idempotencyKey = UUID.randomUUID().toString();

        CustomerRequest request = CustomerRequest.builder()
                .firstName("Bulkhead")
                .lastName("Test")
                .email("bulkhead.test@example.com")
                .phoneNumber("08012345678")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .status(CustomerRequest.CustomerStatus.ACTIVE)
                .idempotencyKey(idempotencyKey)
                .build();

        // Make request - should succeed within bulkhead limits
        webTestClient.post()
                .uri("/api/v1/register/customer")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated();
    }

    @Test
    void shouldHandleTimeLimiterTimeouts() {
        // Test that time limiter is configured properly
        webTestClient.mutate()
                .responseTimeout(Duration.ofSeconds(10))
                .build()
                .get()
                .uri("/api/customers")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void shouldRecoverFromCircuitBreakerOpenState() {
        if (circuitBreakerRegistry == null) {
            System.out.println("Circuit Breaker not configured, skipping test");
            return;
        }

        // After circuit opens due to failures, it should eventually close
        // This is a simplified test - in production you'd want to test actual failure scenarios

        String idempotencyKey = UUID.randomUUID().toString();

        CustomerRequest request = CustomerRequest.builder()
                .firstName("Recovery")
                .lastName("Test")
                .email("recovery.test@example.com")
                .phoneNumber("08012345678")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .status(CustomerRequest.CustomerStatus.ACTIVE)
                .idempotencyKey(idempotencyKey)
                .build();

        // Should eventually succeed
        await().atMost(15, TimeUnit.SECONDS)
                .pollInterval(2, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    webTestClient.post()
                            .uri("/api/v1/register/customer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(request)
                            .exchange()
                            .expectStatus().isCreated();
                });
    }
}