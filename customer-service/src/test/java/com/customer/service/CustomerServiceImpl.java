package com.customer.service;

import com.customer.service.dto.CustomerRequest;
import com.customer.service.dto.CustomerResponse;
import com.customer.service.exception.CustomerCreationException;
import com.customer.service.exception.CustomerNotFoundException;
import com.customer.service.model.Customer;
import com.customer.service.event.CustomerEvent;
import com.customer.service.repository.CustomerRepository;
import com.customer.service.services.CustomerServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;

// 1. Unit Test for CustomerService
@ExtendWith(MockitoExtension.class)
 class CustomerServiceImplTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private KafkaTemplate<String, CustomerEvent> kafkaTemplate;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private CustomerServiceImpl customerService;

    private Customer testCustomer;
    private CustomerRequest testRequest;

    @BeforeEach
    void setUp() {
        testCustomer = Customer.builder()
                .id("507f1f77bcf86cd799439011")  // MongoDB ObjectId format
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

        testRequest = CustomerRequest.builder()
                .tenantId("tenant123")
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .phoneNumber("+1234567890")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .build();
    }

    @Test
    @DisplayName("Should register customer successfully")
    void registerCustomer_Success() {
        // Given
        when(customerRepository.save(any(Customer.class)))
                .thenReturn(Mono.just(testCustomer));

        // When
        Mono<CustomerResponse> result = customerService.registerCustomer(testRequest);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertNotNull(response);
                    assertEquals("John", response.getFirstName());
                    assertEquals("Doe", response.getLastName());
                    assertEquals("john.doe@example.com", response.getEmail());
                    assertEquals("Customer registered successfully", response.getMessage());
                })
                .verifyComplete();

        verify(customerRepository, times(1)).save(any(Customer.class));
    }

//    private <T> Mono when(Mono<Customer> save) {
//        return null;
//    }

    @Test
    @DisplayName("Should throw exception when registration fails")
    void registerCustomer_Failure() {
        // Given
        when(customerRepository.save(any(Customer.class)))
                .thenReturn(Mono.error(new RuntimeException("Database error")));

        // When
        Mono<CustomerResponse> result = customerService.registerCustomer(testRequest);

        // Then
        StepVerifier.create(result)
                .expectError(CustomerCreationException.class)
                .verify();
    }

    @Test
    @DisplayName("Should get customer by ID successfully")
    void getCustomerById_Success() {
        // Given
        when(customerRepository.findById("507f1f77bcf86cd799439011"))
                .thenReturn(Mono.just(testCustomer));

        // When
        Mono<CustomerResponse> result = customerService.getCustomerById("507f1f77bcf86cd799439011");

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertNotNull(response);
                    assertEquals("507f1f77bcf86cd799439011", response.getId());
                    assertEquals("John", response.getFirstName());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should throw exception when customer not found")
    void getCustomerById_NotFound() {
        // Given
        when(customerRepository.findById("nonexistent"))
                .thenReturn(Mono.empty());

        // When
        Mono<CustomerResponse> result = customerService.getCustomerById("nonexistent");

        // Then
        StepVerifier.create(result)
                .expectError(CustomerNotFoundException.class)
                .verify();
    }

    @Test
    @DisplayName("Should update customer successfully")
    void updateCustomer_Success() {
        // Given
        CustomerRequest updateRequest = CustomerRequest.builder()
                .tenantId("tenant123")
                .firstName("Jane")
                .lastName("Smith")
                .email("jane.smith@example.com")
                .phoneNumber("+1234567890")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .build();

        Customer updatedCustomer = Customer.builder()
                .id("507f1f77bcf86cd799439011")
                .tenantId("tenant123")
                .firstName("Jane")
                .lastName("Smith")
                .email("jane.smith@example.com")
                .phoneNumber("+1234567890")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .status(Customer.CustomerStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .accountIds(new ArrayList<>())
                .build();

        when(customerRepository.findById("507f1f77bcf86cd799439011"))
                .thenReturn(Mono.just(testCustomer));
        when(customerRepository.save(any(Customer.class)))
                .thenReturn(Mono.just(updatedCustomer));

        // When
        Mono<CustomerResponse> result = customerService.updateCustomer("507f1f77bcf86cd799439011", updateRequest);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals("Jane", response.getFirstName());
                    assertEquals("Smith", response.getLastName());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should deactivate customer successfully")
    void deactivateCustomer_Success() {
        // Given
        Customer deactivatedCustomer = Customer.builder()
                .id("507f1f77bcf86cd799439011")
                .tenantId("tenant123")
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .phoneNumber("+1234567890")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .status(Customer.CustomerStatus.INACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .accountIds(new ArrayList<>())
                .build();

        when(customerRepository.findById("507f1f77bcf86cd799439011"))
                .thenReturn(Mono.just(testCustomer));
        when(customerRepository.save(any(Customer.class)))
                .thenReturn(Mono.just(deactivatedCustomer));

        // When
        Mono<CustomerResponse> result = customerService.deactivateCustomer("507f1f77bcf86cd799439011");

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(Customer.CustomerStatus.INACTIVE, response.getStatus());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should get all customers successfully")
    void getAllCustomers_Success() {
        // Given
        Customer customer2 = Customer.builder()
                .id("507f1f77bcf86cd799439012")
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

        when(customerRepository.findAll())
                .thenReturn(Flux.just(testCustomer, customer2));

        // When
        Flux<CustomerResponse> result = customerService.getAllCustomers();

        // Then
        StepVerifier.create(result)
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should get customers by tenant successfully")
    void getCustomersByTenant_Success() {
        // Given
        when(customerRepository.findByTenantId("tenant123"))
                .thenReturn(Flux.just(testCustomer));

        // When
        Flux<CustomerResponse> result = customerService.getCustomersByTenant("tenant123");

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals("tenant123", response.getTenantId());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should check if email exists")
    void existsByEmail_Success() {
        // Given
        when(customerRepository.existsByEmail("john.doe@example.com"))
                .thenReturn(Mono.just(true));

        // When
        Mono<Boolean> result = customerService.existsByEmail("john.doe@example.com");

        // Then
        StepVerifier.create(result)
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should add account to customer successfully")
    void addAccountToCustomer_Success() {
        // Given
        Customer customerWithAccount = Customer.builder()
                .id("507f1f77bcf86cd799439011")
                .tenantId("tenant123")
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .phoneNumber("+1234567890")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .status(Customer.CustomerStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .accountIds(new ArrayList<>(Arrays.asList("account123")))
                .build();

        when(customerRepository.findById("507f1f77bcf86cd799439011"))
                .thenReturn(Mono.just(testCustomer));
        when(customerRepository.save(any(Customer.class)))
                .thenReturn(Mono.just(customerWithAccount));

        // When
        Mono<CustomerResponse> result = customerService.addAccountToCustomer("507f1f77bcf86cd799439011", "account123");

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertTrue(response.getAccountIds().contains("account123"));
                })
                .verifyComplete();
    }
}
