package com.customer.service;

import com.commonlib.service.GlobalRateLimiter;
import com.commonlib.service.IdempotencyService;
import com.customer.service.dto.CustomerRequest;
import com.customer.service.dto.CustomerResponse;
import com.customer.service.event.CustomerEvent;
import com.customer.service.exception.CustomerCreationException;
import com.customer.service.exception.CustomerNotFoundException;
import com.customer.service.model.Customer;
import com.customer.service.repository.CustomerRepository;
import com.customer.service.services.CustomerServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CustomerServiceTest {

    @Mock private CustomerRepository       customerRepository;
    @Mock private IdempotencyService       idempotencyService;
    @Mock private GlobalRateLimiter        globalRateLimiter;
    @Mock private RabbitTemplate           rabbitTemplate;
    @Mock private KafkaTemplate<String, CustomerEvent> kafkaTemplate;
    @Mock private SimpMessagingTemplate    simpMessagingTemplate;

    private CustomerServiceImpl customerService;

    private static final String IDEM_KEY  = "key123";
    private static final String CUSTOMER_ID = "69209e767d4c97a3c376051b";

    private CustomerRequest  baseRequest;
    private CustomerResponse cachedResponse;
    private Customer savedCustomer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        customerService = new CustomerServiceImpl(
                customerRepository,
                kafkaTemplate,
                rabbitTemplate,
                simpMessagingTemplate,
                idempotencyService,
                globalRateLimiter
        );

        baseRequest = CustomerRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .phoneNumber("08012345678")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .build();

        savedCustomer = Customer.builder()
                .id(CUSTOMER_ID)
                .tenantId("c1d2b5ec-bdd3-4c30-b73b-ad0f1c573ed9")
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .phoneNumber("08012345678")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .status(Customer.CustomerStatus.ACTIVE)
                .createdAt(Instant.parse("2025-11-21T17:16:38.359926200Z"))
                .updatedAt(Instant.parse("2025-11-21T17:16:38.359926200Z"))
                .accountIds(new ArrayList<>())
                .build();

        cachedResponse = CustomerResponse.builder()
                .id(CUSTOMER_ID)
                .tenantId("c1d2b5ec-bdd3-4c30-b73b-ad0f1c573ed9")
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .phoneNumber("08012345678")
                .message("Customer registered successfully")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .status(Customer.CustomerStatus.ACTIVE)
                .createdAt(Instant.parse("2025-11-21T17:16:38.359926200Z"))
                .updatedAt(Instant.parse("2025-11-21T17:16:38.359926200Z"))
                .accountIds(new ArrayList<>())
                .build();
    }

    // ─────────────────────────────────────────────────────────
    // registerCustomer
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("registerCustomer - returns cached response when idempotency key exists")
    void registerCustomer_IdempotencyExists_ReturnsCached() {
        when(idempotencyService.exists(IDEM_KEY)).thenReturn(Mono.just(true));
        when(idempotencyService.getResponse(IDEM_KEY, CustomerResponse.class))
                .thenReturn(Mono.just(cachedResponse));

        StepVerifier.create(customerService.registerCustomer(baseRequest, IDEM_KEY))
                .expectNext(cachedResponse)
                .verifyComplete();
    }

    @Test
    @DisplayName("registerCustomer - saves new customer when key is fresh and email is new")
    void registerCustomer_NewCustomer_SavesAndStoresIdempotency() {
        when(idempotencyService.exists(IDEM_KEY)).thenReturn(Mono.just(false));
        when(customerRepository.findByEmail("john.doe@example.com")).thenReturn(Mono.empty());
        when(customerRepository.save(any(Customer.class))).thenReturn(Mono.just(savedCustomer));
        when(idempotencyService.storeResponse(eq(IDEM_KEY), any(CustomerResponse.class)))
                .thenReturn(Mono.empty());

        StepVerifier.create(customerService.registerCustomer(baseRequest, IDEM_KEY))
                .expectNextMatches(res ->
                        res.getEmail().equals("john.doe@example.com") &&
                                res.getMessage().equals("Customer registered successfully") &&
                                res.getStatus() == Customer.CustomerStatus.ACTIVE
                )
                .verifyComplete();
    }

    @Test
    @DisplayName("registerCustomer - returns existing customer when email already registered")
    void registerCustomer_EmailAlreadyExists_ReturnsExisting() {
        when(idempotencyService.exists(IDEM_KEY)).thenReturn(Mono.just(false));
        when(customerRepository.findByEmail("john.doe@example.com"))
                .thenReturn(Mono.just(savedCustomer));
        when(idempotencyService.storeResponse(eq(IDEM_KEY), any(CustomerResponse.class)))
                .thenReturn(Mono.empty());

        StepVerifier.create(customerService.registerCustomer(baseRequest, IDEM_KEY))
                .expectNextMatches(res ->
                        res.getMessage().equals("Customer already registered") &&
                                res.getEmail().equals("john.doe@example.com")
                )
                .verifyComplete();
    }

    @Test
    @DisplayName("registerCustomer - throws error when idempotency key is null")
    void registerCustomer_NullKey_ThrowsError() {
        StepVerifier.create(customerService.registerCustomer(baseRequest, null))
                .expectErrorMatches(e ->
                        e instanceof RuntimeException &&
                                e.getMessage().contains("Idempotency-Key")
                )
                .verify();
    }

    @Test
    @DisplayName("registerCustomer - throws error when idempotency key is blank")
    void registerCustomer_BlankKey_ThrowsError() {
        StepVerifier.create(customerService.registerCustomer(baseRequest, "  "))
                .expectErrorMatches(e ->
                        e instanceof RuntimeException &&
                                e.getMessage().contains("Idempotency-Key")
                )
                .verify();
    }

    @Test
    @DisplayName("registerCustomer - wraps DB failure in CustomerCreationException")
    void registerCustomer_DbFailure_WrapsException() {
        when(idempotencyService.exists(IDEM_KEY)).thenReturn(Mono.just(false));
        when(customerRepository.findByEmail(any())).thenReturn(Mono.empty());
        when(customerRepository.save(any(Customer.class)))
                .thenReturn(Mono.error(new RuntimeException("DB timeout")));

        StepVerifier.create(customerService.registerCustomer(baseRequest, IDEM_KEY))
                .expectErrorMatches(e ->
                        e instanceof CustomerCreationException &&
                                e.getMessage().contains("DB timeout")
                )
                .verify();
    }

    // ─────────────────────────────────────────────────────────
    // getCustomerById
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("getCustomerById - returns mapped response when customer exists")
    void getCustomerById_Found_ReturnsMappedResponse() {
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Mono.just(savedCustomer));

        StepVerifier.create(customerService.getCustomerById(CUSTOMER_ID))
                .expectNextMatches(res ->
                        res.getId().equals(CUSTOMER_ID) &&
                                res.getFirstName().equals("John") &&
                                res.getEmail().equals("john.doe@example.com")
                )
                .verifyComplete();
    }

    @Test
    @DisplayName("getCustomerById - throws CustomerNotFoundException when not found")
    void getCustomerById_NotFound_ThrowsException() {
        when(customerRepository.findById("999")).thenReturn(Mono.empty());

        StepVerifier.create(customerService.getCustomerById("999"))
                .expectError(CustomerNotFoundException.class)
                .verify();
    }

    // ─────────────────────────────────────────────────────────
    // getAllCustomers
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAllCustomers - maps Flux of Customer entities to CustomerResponse")
    void getAllCustomers_MapsCorrectly() {
        when(customerRepository.findAll()).thenReturn(Flux.just(savedCustomer));

        StepVerifier.create(customerService.getAllCustomers())
                .expectNextMatches(res ->
                        res.getFirstName().equals("John") &&
                                res.getLastName().equals("Doe") &&
                                res.getEmail().equals("john.doe@example.com") &&
                                res.getStatus() == Customer.CustomerStatus.ACTIVE
                )
                .verifyComplete();
    }

    @Test
    @DisplayName("getAllCustomers - returns empty Flux when no customers exist")
    void getAllCustomers_Empty_ReturnsEmptyFlux() {
        when(customerRepository.findAll()).thenReturn(Flux.empty());

        StepVerifier.create(customerService.getAllCustomers())
                .verifyComplete();
    }

    // ─────────────────────────────────────────────────────────
    // updateCustomer
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateCustomer - updates fields and refreshes updatedAt")
    void updateCustomer_UpdatesFieldsCorrectly() {
        CustomerRequest updateReq = CustomerRequest.builder()
                .firstName("Jane")
                .lastName("Doe")
                .email("jane.doe@example.com")
                .phoneNumber("08099999999")
                .dateOfBirth(LocalDate.of(1992, 5, 15))
                .build();

        Customer updatedCustomer = savedCustomer.toBuilder()
                .firstName("Jane")
                .email("jane.doe@example.com")
                .updatedAt(Instant.now())
                .build();

        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Mono.just(savedCustomer));
        when(customerRepository.save(any(Customer.class))).thenReturn(Mono.just(updatedCustomer));

        StepVerifier.create(customerService.updateCustomer(CUSTOMER_ID, updateReq))
                .expectNextMatches(res -> res.getFirstName().equals("Jane"))
                .verifyComplete();
    }

    @Test
    @DisplayName("updateCustomer - throws CustomerNotFoundException when not found")
    void updateCustomer_NotFound_ThrowsException() {
        when(customerRepository.findById("nonexistent")).thenReturn(Mono.empty());

        StepVerifier.create(customerService.updateCustomer("nonexistent", baseRequest))
                .expectError(CustomerNotFoundException.class)
                .verify();
    }

    // ─────────────────────────────────────────────────────────
    // deactivateCustomer
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("deactivateCustomer - sets status to INACTIVE")
    void deactivateCustomer_SetsStatusInactive() {
        Customer deactivated = savedCustomer.toBuilder()
                .status(Customer.CustomerStatus.INACTIVE)
                .build();

        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Mono.just(savedCustomer));
        when(customerRepository.save(any(Customer.class))).thenReturn(Mono.just(deactivated));

        StepVerifier.create(customerService.deactivateCustomer(CUSTOMER_ID))
                .expectNextMatches(res ->
                        res.getStatus() == Customer.CustomerStatus.INACTIVE
                )
                .verifyComplete();
    }

    // ─────────────────────────────────────────────────────────
    // deleteCustomer
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteCustomer - completes Mono<Void> on success")
    void deleteCustomer_CompletesSuccessfully() {
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Mono.just(savedCustomer));
        when(customerRepository.deleteById(CUSTOMER_ID)).thenReturn(Mono.empty());

        StepVerifier.create(customerService.deleteCustomer(CUSTOMER_ID))
                .verifyComplete();
    }

    // ─────────────────────────────────────────────────────────
    // getCustomersByTenant
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("getCustomersByTenant - filters customers by tenantId")
    void getCustomersByTenant_FiltersCorrectly() {
        String tenantId = "c1d2b5ec-bdd3-4c30-b73b-ad0f1c573ed9";
        when(customerRepository.findByTenantId(tenantId))
                .thenReturn(Flux.just(savedCustomer));

        StepVerifier.create(customerService.getCustomersByTenant(tenantId))
                .expectNextMatches(res ->
                        res.getTenantId().equals(tenantId) &&
                                res.getEmail().equals("john.doe@example.com")
                )
                .verifyComplete();
    }
}