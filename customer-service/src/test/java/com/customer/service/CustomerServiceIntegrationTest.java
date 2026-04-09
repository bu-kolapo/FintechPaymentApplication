package com.customer.service;

import com.customer.service.dto.CustomerRequest;
import com.customer.service.dto.CustomerResponse;
import com.customer.service.model.Customer;
import org.junit.jupiter.api.BeforeEach;
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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class CustomerServiceIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private WebTestClient webTestClient;

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

        // Disable resilience4j features for integration tests to avoid flakiness
        registry.add("resilience4j.circuitbreaker.configs.default.enabled", () -> "false");
        registry.add("resilience4j.bulkhead.configs.default.enabled", () -> "false");
        registry.add("resilience4j.ratelimiter.configs.default.enabled", () -> "false");
        registry.add("resilience4j.retry.configs.default.enabled", () -> "false");
        registry.add("resilience4j.timelimiter.configs.default.enabled", () -> "false");
    }

    @BeforeEach
    void setUp() {
        webTestClient = webTestClient.mutate()
                .responseTimeout(Duration.ofSeconds(30))
                .build();
    }

    @Test
    void shouldRegisterCustomer() {
        String idempotencyKey = UUID.randomUUID().toString();

        CustomerRequest request = CustomerRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .phoneNumber("08012345678")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .status(CustomerRequest.CustomerStatus.ACTIVE)
                .build();

        webTestClient.post()
                .uri("/api/v1/register/customer")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(CustomerResponse.class)
                .value(response -> {
                    assertThat(response.getFirstName()).isEqualTo("John");
                    assertThat(response.getLastName()).isEqualTo("Doe");
                    assertThat(response.getEmail()).isEqualTo("john.doe@example.com");
                    assertThat(response.getPhoneNumber()).isEqualTo("08012345678");
                    assertThat(response.getDateOfBirth()).isEqualTo(LocalDate.of(1990, 1, 1));
                    assertThat(response.getStatus()).isEqualTo(Customer.CustomerStatus.ACTIVE);
                    assertThat(response.getId()).isNotNull();
                    assertThat(response.getTenantId()).isNotNull();
                    assertThat(response.getCreatedAt()).isNotNull();
                    assertThat(response.getUpdatedAt()).isNotNull();
                    assertThat(response.getMessage()).isEqualTo("Customer registered successfully");
                });
    }

    @Test
    void shouldReturnCachedResponseForDuplicateIdempotencyKey() {
        String idempotencyKey = UUID.randomUUID().toString();

        CustomerRequest request = CustomerRequest.builder()
                .firstName("Jane")
                .lastName("Smith")
                .email("jane.smith@example.com")
                .phoneNumber("08087654321")
                .dateOfBirth(LocalDate.of(1985, 5, 15))
                .status(CustomerRequest.CustomerStatus.ACTIVE)
                .build();

        // First registration
        CustomerResponse firstResponse = webTestClient.post()
                .uri("/api/v1/register/customer")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(CustomerResponse.class)
                .returnResult()
                .getResponseBody();

        // Second registration with same idempotency key should return cached response
        webTestClient.post()
                .uri("/api/v1/register/customer")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(CustomerResponse.class)
                .value(response -> {
                    assertThat(response.getId()).isEqualTo(firstResponse.getId());
                    assertThat(response.getEmail()).isEqualTo(firstResponse.getEmail());
                    assertThat(response.getCreatedAt()).isEqualTo(firstResponse.getCreatedAt());
                });
    }

    @Test
    void shouldGetAllCustomers() {
        // Create a customer first
        String idempotencyKey = UUID.randomUUID().toString();

        CustomerRequest request = CustomerRequest.builder()
                .firstName("Alice")
                .lastName("Johnson")
                .email("alice.johnson@example.com")
                .phoneNumber("08011112222")
                .dateOfBirth(LocalDate.of(1992, 3, 20))
                .status(CustomerRequest.CustomerStatus.ACTIVE)
                .build();

        webTestClient.post()
                .uri("/api/v1/register/customer")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated();

        // Get all customers
        webTestClient.get()
                .uri("/api/customers")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(CustomerResponse.class)
                .value(customers -> {
                    assertThat(customers).isNotEmpty();
                    assertThat(customers).anyMatch(c ->
                            c.getEmail().equals("alice.johnson@example.com") &&
                                    c.getFirstName().equals("Alice") &&
                                    c.getLastName().equals("Johnson")
                    );
                });
    }

    @Test
    void shouldGetCustomerById() {
        // Create a customer
        String idempotencyKey = UUID.randomUUID().toString();

        CustomerRequest request = CustomerRequest.builder()
                .firstName("Bob")
                .lastName("Williams")
                .email("bob.williams@example.com")
                .phoneNumber("08033334444")
                .dateOfBirth(LocalDate.of(1988, 7, 10))
                .status(CustomerRequest.CustomerStatus.ACTIVE)
                .build();

        CustomerResponse created = webTestClient.post()
                .uri("/api/v1/register/customer")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(CustomerResponse.class)
                .returnResult()
                .getResponseBody();

        // Get customer by ID
        webTestClient.get()
                .uri("/api/customers/" + created.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody(CustomerResponse.class)
                .value(response -> {
                    assertThat(response.getId()).isEqualTo(created.getId());
                    assertThat(response.getEmail()).isEqualTo("bob.williams@example.com");
                    assertThat(response.getFirstName()).isEqualTo("Bob");
                    assertThat(response.getLastName()).isEqualTo("Williams");
                    assertThat(response.getPhoneNumber()).isEqualTo("08033334444");
                });
    }

    @Test
    void shouldReturnNotFoundForNonExistentCustomer() {
        webTestClient.get()
                .uri("/api/customers/non-existent-id-12345")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void shouldUpdateCustomer() {
        // Create a customer
        String idempotencyKey = UUID.randomUUID().toString();

        CustomerRequest createRequest = CustomerRequest.builder()
                .firstName("Charlie")
                .lastName("Brown")
                .email("charlie.brown@example.com")
                .phoneNumber("08055556666")
                .dateOfBirth(LocalDate.of(1995, 9, 25))
                .status(CustomerRequest.CustomerStatus.ACTIVE)
                .build();

        CustomerResponse created = webTestClient.post()
                .uri("/api/v1/register/customer")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(createRequest)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(CustomerResponse.class)
                .returnResult()
                .getResponseBody();

        // Update customer
        CustomerRequest updateRequest = CustomerRequest.builder()
                .firstName("Charlie")
                .lastName("Smith")
                .email("charlie.brown@example.com")
                .phoneNumber("08099998888")
                .dateOfBirth(LocalDate.of(1995, 9, 25))
                .status(CustomerRequest.CustomerStatus.ACTIVE)
                .build();

        webTestClient.put()
                .uri("/api/customers/" + created.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updateRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(CustomerResponse.class)
                .value(response -> {
                    assertThat(response.getLastName()).isEqualTo("Smith");
                    assertThat(response.getPhoneNumber()).isEqualTo("08099998888");
                    assertThat(response.getFirstName()).isEqualTo("Charlie");
                });
    }

    @Test
    void shouldDeleteCustomer() {
        // Create a customer
        String idempotencyKey = UUID.randomUUID().toString();

        CustomerRequest request = CustomerRequest.builder()
                .firstName("David")
                .lastName("Davis")
                .email("david.davis@example.com")
                .phoneNumber("08077778888")
                .dateOfBirth(LocalDate.of(1993, 11, 30))
                .status(CustomerRequest.CustomerStatus.ACTIVE)
                .build();

        CustomerResponse created = webTestClient.post()
                .uri("/api/v1/register/customer")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(CustomerResponse.class)
                .returnResult()
                .getResponseBody();

        // Delete customer
        webTestClient.delete()
                .uri("/api/customers/" + created.getId())
                .exchange()
                .expectStatus().isNoContent();

        // Verify customer is deleted
        webTestClient.get()
                .uri("/api/customers/" + created.getId())
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void shouldValidateCustomerRequest_MissingRequiredFields() {
        CustomerRequest invalidRequest = CustomerRequest.builder()
                .firstName("")
                .lastName("")
                .email("invalid-email")
                .build();

        webTestClient.post()
                .uri("/api/v1/register/customer")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalidRequest)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void shouldValidateCustomerRequest_InvalidEmail() {
        String idempotencyKey = UUID.randomUUID().toString();

        CustomerRequest invalidRequest = CustomerRequest.builder()
                .firstName("Test")
                .lastName("User")
                .email("not-an-email")
                .phoneNumber("08012345678")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .status(CustomerRequest.CustomerStatus.ACTIVE)
                .build();

        webTestClient.post()
                .uri("/api/v1/register/customer")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalidRequest)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void shouldHandleInvalidCustomerStatus() {
        String idempotencyKey = UUID.randomUUID().toString();

        CustomerRequest request = CustomerRequest.builder()
                .firstName("Test")
                .lastName("User")
                .email("test@example.com")
                .phoneNumber("08012345678")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .status(CustomerRequest.CustomerStatus.INACTIVE)
                .build();

        webTestClient.post()
                .uri("/api/v1/register/customer")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();
    }
}