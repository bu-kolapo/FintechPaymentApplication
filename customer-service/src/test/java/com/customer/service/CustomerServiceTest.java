package com.customer.service;

import com.commonlib.service.IdempotencyService;
import com.customer.service.dto.CustomerRequest;
import com.customer.service.model.Customer;
import com.customer.service.dto.CustomerResponse;
import com.customer.service.event.CustomerEvent;
import com.customer.service.exception.CustomerNotFoundException;
import com.customer.service.repository.CustomerRepository;
import com.customer.service.services.CustomerServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import static org.mockito.Mockito.when;
import reactor.core.publisher.Flux;



import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;


@ExtendWith(MockitoExtension.class)
 public class CustomerServiceTest {
    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private IdempotencyService idempotencyService;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private KafkaTemplate<String, CustomerEvent> kafkaTemplate;

    @Mock
    private SimpMessagingTemplate simpMessagingTemplate;

    private CustomerServiceImpl customerService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Manually inject all dependencies in the correct order
        customerService = new CustomerServiceImpl(
                customerRepository,
                kafkaTemplate,
                rabbitTemplate,
                simpMessagingTemplate,
                idempotencyService
        );
    }

    @Test
    void testRegisterCustomer_IdempotencyExists() {
        CustomerRequest request = CustomerRequest.builder()
                .idempotencyKey("key123")
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .phoneNumber("08012345678")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .build();

        CustomerResponse cachedResponse = CustomerResponse.builder()
                .id("69209e767d4c97a3c376051b")
                .tenantId("c1d2b5ec-bdd3-4c30-b73b-ad0f1c573ed9")
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .phoneNumber("08012345678")
                .message("Customer registered successfully")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .status(com.customer.service.model.Customer.CustomerStatus.ACTIVE)
                .createdAt(Instant.parse("2025-11-21T17:16:38.359926200Z"))
                .updatedAt(Instant.parse("2025-11-21T17:16:38.359926200Z"))
                .accountIds(new ArrayList<>())
                .build();

        Mockito.when(idempotencyService.exists("key123")).thenReturn(Mono.just(true));
        Mockito.when(idempotencyService.getResponse("key123", CustomerResponse.class)).thenReturn(Mono.just(cachedResponse));

        StepVerifier.create(customerService.registerCustomer(request))
                .expectNext(cachedResponse)
                .verifyComplete();
    }


    @Test
    void testGetCustomerById_NotFound() {
        Mockito.when(customerRepository.findById("999")).thenReturn(Mono.empty());

        StepVerifier.create(customerService.getCustomerById("999"))
                .expectError(CustomerNotFoundException.class)
                .verify();
    }


    @Test
    void testGetAllCustomers() {
        // Create entity Customer (not model Customer)
        Customer customer = Customer.builder()
                .id("1")
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .status(Customer.CustomerStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .accountIds(new ArrayList<>())
                .build();

        when(customerRepository.findAll()).thenReturn(Flux.just(customer));

        StepVerifier.create(customerService.getAllCustomers())
                .expectNextMatches(res ->
                        res.getFirstName().equals("John") &&
                                res.getLastName().equals("Doe") &&
                                res.getEmail().equals("john@example.com") &&
                                res.getStatus() == com.customer.service.model.Customer.CustomerStatus.ACTIVE
                )
                .verifyComplete();
    }


}

