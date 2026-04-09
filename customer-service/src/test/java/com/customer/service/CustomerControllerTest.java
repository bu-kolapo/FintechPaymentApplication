package com.customer.service;

import com.customer.service.controller.CustomerController;
import com.customer.service.dto.CustomerRequest;
import com.customer.service.dto.CustomerResponse;
import com.customer.service.exception.CustomerCreationException;
import com.customer.service.exception.CustomerNotFoundException;
import com.customer.service.model.Customer;
import com.customer.service.services.CustomerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@WebFluxTest(CustomerController.class)
public class CustomerControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private CustomerService customerService;

    private static final String BASE_URI    = "/api/v1/customers";
    private static final String IDEM_KEY    = "157f73c8-14da-4924-9358-9e6ca8b77025";
    private static final String IDEM_HEADER = "Idempotency-Key";

    private CustomerRequest  testRequest;
    private CustomerResponse testResponse;

    @BeforeEach
    void setUp() {
        testRequest = CustomerRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .phoneNumber("08012345678")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .build();

        testResponse = CustomerResponse.builder()
                .id("507f1f77bcf86cd799439011")
                .tenantId("c1d2b5ec-bdd3-4c30-b73b-ad0f1c573ed9")
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .phoneNumber("08012345678")
                .message("Customer registered successfully")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .status(Customer.CustomerStatus.ACTIVE)
                .createdAt(Instant.parse("2025-01-01T00:00:00Z"))
                .updatedAt(Instant.parse("2025-01-01T00:00:00Z"))
                .accountIds(new ArrayList<>())
                .build();
    }

    // ─────────────────────────────────────────────────────────
    // POST /register/customer
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /register/customer - 201 Created on new registration")
    void registerCustomer_Success() {
        when(customerService.registerCustomer(any(CustomerRequest.class), eq(IDEM_KEY)))
                .thenReturn(Mono.just(testResponse));

        webTestClient.post()
                .uri(BASE_URI + "/register/customer")
                .contentType(MediaType.APPLICATION_JSON)
                .header(IDEM_HEADER, IDEM_KEY)
                .bodyValue(testRequest)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isEqualTo("507f1f77bcf86cd799439011")
                .jsonPath("$.firstName").isEqualTo("John")
                .jsonPath("$.lastName").isEqualTo("Doe")
                .jsonPath("$.email").isEqualTo("john.doe@example.com")
                .jsonPath("$.message").isEqualTo("Customer registered successfully")
                .jsonPath("$.status").isEqualTo("ACTIVE")
                .jsonPath("$.accountIds").isArray();
    }

    @Test
    @DisplayName("POST /register/customer - 201 Created on idempotent replay (Redis hit)")
    void registerCustomer_IdempotentReplay_ReturnsCached() {
        CustomerResponse cachedResponse = testResponse.toBuilder()
                .message("Customer registered successfully")
                .build();

        when(customerService.registerCustomer(any(CustomerRequest.class), eq(IDEM_KEY)))
                .thenReturn(Mono.just(cachedResponse));

        webTestClient.post()
                .uri(BASE_URI + "/register/customer")
                .contentType(MediaType.APPLICATION_JSON)
                .header(IDEM_HEADER, IDEM_KEY)
                .bodyValue(testRequest)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isEqualTo("507f1f77bcf86cd799439011")
                .jsonPath("$.message").isEqualTo("Customer registered successfully");
    }

    @Test
    @DisplayName("POST /register/customer - 400 Bad Request when Idempotency-Key header is missing")
    void registerCustomer_MissingIdempotencyHeader_Returns400() {
        webTestClient.post()
                .uri(BASE_URI + "/register/customer")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(testRequest)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("POST /register/customer - 500 when CustomerCreationException is thrown")
    void registerCustomer_CreationException_Returns500() {
        when(customerService.registerCustomer(any(CustomerRequest.class), eq(IDEM_KEY)))
                .thenReturn(Mono.error(new CustomerCreationException(
                        "Failed to register customer: DB unavailable", null)));

        webTestClient.post()
                .uri(BASE_URI + "/register/customer")
                .contentType(MediaType.APPLICATION_JSON)
                .header(IDEM_HEADER, IDEM_KEY)
                .bodyValue(testRequest)
                .exchange()
                .expectStatus().is5xxServerError();
    }

    // ─────────────────────────────────────────────────────────
    // GET /register/customer/{id}
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /register/customer/{id} - 200 OK when customer exists")
    void getCustomerById_Found_Returns200() {
        when(customerService.getCustomerById("507f1f77bcf86cd799439011"))
                .thenReturn(Mono.just(testResponse));

        webTestClient.get()
                .uri(BASE_URI + "/register/customer/507f1f77bcf86cd799439011")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo("507f1f77bcf86cd799439011")
                .jsonPath("$.email").isEqualTo("john.doe@example.com");
    }

    @Test
    @DisplayName("GET /register/customer/{id} - 404 when customer not found")
    void getCustomerById_NotFound_Returns404() {
        when(customerService.getCustomerById("nonexistent"))
                .thenReturn(Mono.error(new CustomerNotFoundException("Customer not found")));

        webTestClient.get()
                .uri(BASE_URI + "/register/customer/nonexistent")
                .exchange()
                .expectStatus().isNotFound();
    }

    // ─────────────────────────────────────────────────────────
    // PUT /register/customer/{id}
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("PUT /register/customer/{id} - 200 OK on successful update")
    void updateCustomer_Success_Returns200() {
        CustomerResponse updated = testResponse.toBuilder()
                .firstName("Jane")
                .message("Customer updated successfully")
                .build();

        when(customerService.updateCustomer(eq("507f1f77bcf86cd799439011"),
                any(CustomerRequest.class)))
                .thenReturn(Mono.just(updated));

        webTestClient.put()
                .uri(BASE_URI + "/register/customer/507f1f77bcf86cd799439011")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(testRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.firstName").isEqualTo("Jane")
                .jsonPath("$.message").isEqualTo("Customer updated successfully");
    }

    @Test
    @DisplayName("PUT /register/customer/{id} - 404 when customer not found")
    void updateCustomer_NotFound_Returns404() {
        when(customerService.updateCustomer(eq("nonexistent"), any(CustomerRequest.class)))
                .thenReturn(Mono.error(new CustomerNotFoundException("Customer not found")));

        webTestClient.put()
                .uri(BASE_URI + "/register/customer/nonexistent")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(testRequest)
                .exchange()
                .expectStatus().isNotFound();
    }

    // ─────────────────────────────────────────────────────────
    // DELETE /register/customer/{id}
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("DELETE /register/customer/{id} - 204 No Content on success")
    void deleteCustomer_Success_Returns204() {
        when(customerService.deleteCustomer("507f1f77bcf86cd799439011"))
                .thenReturn(Mono.empty());

        webTestClient.delete()
                .uri(BASE_URI + "/register/customer/507f1f77bcf86cd799439011")
                .exchange()
                .expectStatus().isNoContent();
    }

    // ─────────────────────────────────────────────────────────
    // GET /register/customer (all)
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /register/customer - 200 OK returns list of customers")
    void getAllCustomers_Returns200WithList() {
        CustomerResponse second = testResponse.toBuilder()
                .id("507f1f77bcf86cd799439012")
                .email("jane.doe@example.com")
                .firstName("Jane")
                .build();

        when(customerService.getAllCustomers())
                .thenReturn(Flux.just(testResponse, second));

        webTestClient.get()
                .uri(BASE_URI + "/register/customer")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(CustomerResponse.class)
                .hasSize(2);
    }

    @Test
    @DisplayName("GET /register/customer - 200 OK returns empty list")
    void getAllCustomers_EmptyList_Returns200() {
        when(customerService.getAllCustomers()).thenReturn(Flux.empty());

        webTestClient.get()
                .uri(BASE_URI + "/register/customer")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(CustomerResponse.class)
                .hasSize(0);
    }

    // ─────────────────────────────────────────────────────────
    // PATCH /register/customer/{id}/deactivate
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("PATCH /register/customer/{id}/deactivate - 200 OK sets status INACTIVE")
    void deactivateCustomer_Success_Returns200() {
        CustomerResponse deactivated = testResponse.toBuilder()
                .status(Customer.CustomerStatus.INACTIVE)
                .message("Customer deactivated successfully")
                .build();

        when(customerService.deactivateCustomer("507f1f77bcf86cd799439011"))
                .thenReturn(Mono.just(deactivated));

        webTestClient.patch()
                .uri(BASE_URI + "/register/customer/507f1f77bcf86cd799439011/deactivate")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("INACTIVE")
                .jsonPath("$.message").isEqualTo("Customer deactivated successfully");
    }

    // ─────────────────────────────────────────────────────────
    // GET /register/customer/tenant/{tenantId}
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /register/customer/tenant/{tenantId} - 200 OK filters by tenant")
    void getCustomersByTenant_Returns200() {
        when(customerService.getCustomersByTenant("c1d2b5ec-bdd3-4c30-b73b-ad0f1c573ed9"))
                .thenReturn(Flux.just(testResponse));

        webTestClient.get()
                .uri(BASE_URI + "/register/customer/tenant/c1d2b5ec-bdd3-4c30-b73b-ad0f1c573ed9")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(CustomerResponse.class)
                .hasSize(1);
    }
}