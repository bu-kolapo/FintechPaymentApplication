package com.customer.service;

import com.customer.service.dto.CustomerRequest;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Repository;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;

// 1. MongoDB Repository Integration Test with Testcontainers
@SpringBootTest
@Testcontainers
@TestPropertySource(properties = {
        "spring.data.mongodb.uri=mongodb://localhost:27017/test",
        "messaging.enabled=false"
})
class CustomerRepositoryIntegrationTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0")
            .withExposedPorts(27017);

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    private CustomerRepository customerRepository;

    private Customer testCustomer;

    @BeforeEach
    void setUp() {
        customerRepository.deleteAll().block();

        testCustomer = Customer.builder()
                .tenantId("tenant123")
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .phoneNumber("+1234567890")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .status(Customer.CustomerStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .accountIds(new ArrayList<>())
                .build();
    }

    @Test
    @DisplayName("Should save and retrieve customer from MongoDB")
    void saveAndRetrieveCustomer() {
        // When
        Customer savedCustomer = customerRepository.save(testCustomer).block();

        // Then
        assertNotNull(savedCustomer);
        assertNotNull(savedCustomer.getId());

        Customer retrievedCustomer = customerRepository.findById(savedCustomer.getId()).block();
        assertNotNull(retrievedCustomer);
        assertEquals("John", retrievedCustomer.getFirstName());
        assertEquals("Doe", retrievedCustomer.getLastName());
        assertEquals("john.doe@example.com", retrievedCustomer.getEmail());
    }

    @Test
    @DisplayName("Should find customer by email")
    void findByEmail() {
        // Given
        customerRepository.save(testCustomer).block();

        // When
        Customer found = customerRepository.findByEmail("john.doe@example.com").block();

        // Then
        assertNotNull(found);
        assertEquals("John", found.getFirstName());
    }

    @Test
    @DisplayName("Should find customers by tenant ID")
    void findByTenantId() {
        // Given
        customerRepository.save(testCustomer).block();

        Customer customer2 = Customer.builder()
                .tenantId("tenant123")
                .firstName("Jane")
                .lastName("Smith")
                .email("jane.smith@example.com")
                .phoneNumber("+0987654321")
                .dateOfBirth(LocalDate.of(1992, 5, 15))
                .status(Customer.CustomerStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .accountIds(new ArrayList<>())
                .build();
        customerRepository.save(customer2).block();

        // When
        List<Customer> customers = customerRepository.findByTenantId("tenant123")
                .collectList()
                .block();

        // Then
        assertNotNull(customers);
        assertEquals(2, customers.size());
    }

    @Test
    @DisplayName("Should find customers by status")
    void findByStatus() {
        // Given
        testCustomer.setStatus(Customer.CustomerStatus.INACTIVE);
        customerRepository.save(testCustomer).block();

        // When
        List<Customer> inactiveCustomers = customerRepository
                .findByStatus(Customer.CustomerStatus.INACTIVE)
                .collectList()
                .block();

        // Then
        assertNotNull(inactiveCustomers);
        assertEquals(1, inactiveCustomers.size());
        assertEquals(Customer.CustomerStatus.INACTIVE, inactiveCustomers.get(0).getStatus());
    }

    @Test
    @DisplayName("Should check if email exists")
    void existsByEmail() {
        // Given
        customerRepository.save(testCustomer).block();

        // When
        Boolean exists = customerRepository.existsByEmail("john.doe@example.com").block();

        // Then
        assertNotNull(exists);
        assertTrue(exists);
    }

    @Test
    @DisplayName("Should count customers by tenant")
    void countByTenantId() {
        // Given
        customerRepository.save(testCustomer).block();

        // When
        Long count = customerRepository.countByTenantId("tenant123").block();

        // Then
        assertNotNull(count);
        assertEquals(Optional.of(1L), count);
    }

    @Test
    @DisplayName("Should search customers by name or email")
    void searchCustomers() {
        // Given
        customerRepository.save(testCustomer).block();

        // When
        List<Customer> results = customerRepository
                .findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                        "John", "John", "John")
                .collectList()
                .block();

        // Then
        assertNotNull(results);
        assertTrue(results.size() > 0);
    }
}

// 2. Full Integration Test with all components
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@TestPropertySource(properties = {
        "messaging.enabled=true",
        "messaging.kafka.enabled=true",
        "messaging.rabbitmq.enabled=true"
})
class CustomerServiceFullIntegrationTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0");

    @Container
    static KafkaContainer kafkaContainer = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    @Container
    static RabbitMQContainer rabbitMQContainer = new RabbitMQContainer("rabbitmq:3.12-management");

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
        registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);
        registry.add("spring.rabbitmq.host", rabbitMQContainer::getHost);
        registry.add("spring.rabbitmq.port", rabbitMQContainer::getAmqpPort);
    }

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private CustomerRepository customerRepository;

    @BeforeEach
    void setUp() {
        customerRepository.deleteAll().block();
    }

    @Test
    @DisplayName("Full integration test - Register customer end-to-end")
    void registerCustomerEndToEnd() {
        // Given
        CustomerRequest request = CustomerRequest.builder()
                .tenantId("tenant123")
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .phoneNumber("+1234567890")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .build();

        // When & Then
        webTestClient.post()
                .uri("/api/v1/customers/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isNotEmpty()
                .jsonPath("$.firstName").isEqualTo("John")
                .jsonPath("$.lastName").isEqualTo("Doe")
                .jsonPath("$.email").isEqualTo("john.doe@example.com")
                .jsonPath("$.message").isEqualTo("Customer registered successfully");

        // Verify customer is in database
        Customer savedCustomer = customerRepository
                .findByEmail("john.doe@example.com")
                .block();
        assertNotNull(savedCustomer);
        assertEquals("John", savedCustomer.getFirstName());
    }

    @Test
    @DisplayName("Full integration test - Update customer workflow")
    void updateCustomerWorkflow() {
        // Given - Create a customer first
        Customer customer = Customer.builder()
                .tenantId("tenant123")
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .phoneNumber("+1234567890")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .status(Customer.CustomerStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .accountIds(new ArrayList<>())
                .build();

        Customer savedCustomer = customerRepository.save(customer).block();
        assertNotNull(savedCustomer);

        // When - Update the customer
        CustomerRequest updateRequest = CustomerRequest.builder()
                .tenantId("tenant123")
                .firstName("Jane")
                .lastName("Smith")
                .email("jane.smith@example.com")
                .phoneNumber("+0987654321")
                .dateOfBirth(LocalDate.of(1992, 5, 15))
                .build();

        webTestClient.put()
                .uri("/api/v1/customers/" + savedCustomer.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updateRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.firstName").isEqualTo("Jane")
                .jsonPath("$.lastName").isEqualTo("Smith")
                .jsonPath("$.email").isEqualTo("jane.smith@example.com");

        // Then - Verify update in database
        Customer updatedCustomer = customerRepository
                .findById(savedCustomer.getId())
                .block();
        assertNotNull(updatedCustomer);
        assertEquals("Jane", updatedCustomer.getFirstName());
    }
}

// 3. Test configuration for embedded MongoDB
@TestConfiguration
 class MongoDBTestConfig {

    @Bean
    public MongoTemplate mongoTemplate() throws IOException {
        String connectionString = "mongodb://localhost:27017/test";
        MongoClient mongoClient = MongoClients.create(connectionString);
        return new MongoTemplate(mongoClient, "test");
    }
}

// 4. Customer Entity for MongoDB (ensure you have this)
@Document(collection = "customers")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
class Customer {

    @Id
    private String id;  // MongoDB uses String ID by default

    @Indexed
    private String tenantId;

    private String firstName;
    private String lastName;

    @Indexed(unique = true)
    private String email;

    private String phoneNumber;
    private LocalDate dateOfBirth;

    @Indexed
    private CustomerStatus status;

    private Instant createdAt;
    private Instant updatedAt;
    private List<String> accountIds;

    public enum CustomerStatus {
        ACTIVE, INACTIVE, SUSPENDED
    }
}

// 5. Update CustomerRepository for MongoDB
@Repository
 interface CustomerRepository extends ReactiveMongoRepository<Customer, String> {

    Mono<Customer> findByEmail(String email);

    Flux<Customer> findByTenantId(String tenantId);

    Flux<Customer> findByStatus(Customer.CustomerStatus status);

    Mono<Boolean> existsByEmail(String email);

    Mono<Long> countByTenantId(String tenantId);

    Mono<Long> countByStatus(Customer.CustomerStatus status);

    Flux<Customer> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String firstName, String lastName, String email);
}