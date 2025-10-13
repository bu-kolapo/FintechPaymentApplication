package com.customer.service;

import com.commonlib.exception.GlobalExceptionHandler;
import com.customer.service.controller.CustomerController;
import com.customer.service.dto.CustomerRequest;
import com.customer.service.dto.CustomerResponse;
import com.customer.service.exception.CustomerNotFoundException;
import com.customer.service.model.Customer;
import com.customer.service.services.CustomerService;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@WebFluxTest(CustomerController.class)
@Import(GlobalExceptionHandler.class)
class CustomerControllerIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private CustomerService customerService;

    private CustomerRequest testRequest;
    private CustomerResponse testResponse;

    @BeforeEach
    void setUp() {
        testRequest = CustomerRequest.builder()
                .tenantId("tenant123")
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .phoneNumber("+1234567890")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .build();

        testResponse = CustomerResponse.builder()
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
                .accountIds(new ArrayList<>())
                .message("Customer registered successfully")
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/customers/register - Success")
    void registerCustomer_Success() {
        // Given
        when(customerService.registerCustomer(any(CustomerRequest.class)))
                .thenReturn(Mono.just(testResponse));

        // When & Then
        webTestClient.post()
                .uri("/api/v1/customers/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(testRequest)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isEqualTo("507f1f77bcf86cd799439011")
                .jsonPath("$.firstName").isEqualTo("John")
                .jsonPath("$.lastName").isEqualTo("Doe")
                .jsonPath("$.email").isEqualTo("john.doe@example.com")
                .jsonPath("$.message").isEqualTo("Customer registered successfully");
    }

    @Test
    @DisplayName("POST /api/v1/customers/register - Validation Error")
    void registerCustomer_ValidationError() {
        // Given - Invalid request (missing required fields)
        CustomerRequest invalidRequest = CustomerRequest.builder()
                .firstName("John")
                .build();

        // When & Then
        webTestClient.post()
                .uri("/api/v1/customers/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalidRequest)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("GET /api/v1/customers/{id} - Success")
    void getCustomerById_Success() {
        // Given
        when(customerService.getCustomerById("507f1f77bcf86cd799439011"))
                .thenReturn(Mono.just(testResponse));

        // When & Then
        webTestClient.get()
                .uri("/api/v1/customers/507f1f77bcf86cd799439011")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo("507f1f77bcf86cd799439011")
                .jsonPath("$.firstName").isEqualTo("John");
    }

    @Test
    @DisplayName("GET /api/v1/customers/{id} - Not Found")
    void getCustomerById_NotFound() {
        // Given
        when(customerService.getCustomerById("nonexistent"))
                .thenReturn(Mono.error(new CustomerNotFoundException("Customer not found")));

        // When & Then
        webTestClient.get()
                .uri("/api/v1/customers/nonexistent")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("GET /api/v1/customers - Get All Customers")
    void getAllCustomers_Success() {
        // Given
        when(customerService.getAllCustomers())
                .thenReturn(Flux.just(testResponse));

        // When & Then
        webTestClient.get()
                .uri("/api/v1/customers")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(CustomerResponse.class)
                .hasSize(1);
    }

    @Test
    @DisplayName("PUT /api/v1/customers/{id} - Update Success")
    void updateCustomer_Success() {
        // Given
        CustomerResponse updatedResponse = CustomerResponse.builder()
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
                .message("Customer updated successfully")
                .build();

        when(customerService.updateCustomer(eq("507f1f77bcf86cd799439011"), any(CustomerRequest.class)))
                .thenReturn(Mono.just(updatedResponse));

        // When & Then
        webTestClient.put()
                .uri("/api/v1/customers/507f1f77bcf86cd799439011")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(testRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.firstName").isEqualTo("Jane")
                .jsonPath("$.lastName").isEqualTo("Smith");
    }

    @Test
    @DisplayName("DELETE /api/v1/customers/{id} - Success")
    void deleteCustomer_Success() {
        // Given
        when(customerService.deleteCustomer("507f1f77bcf86cd799439011"))
                .thenReturn(Mono.empty());

        // When & Then
        webTestClient.delete()
                .uri("/api/v1/customers/507f1f77bcf86cd799439011")
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    @DisplayName("PATCH /api/v1/customers/{id}/deactivate - Success")
    void deactivateCustomer_Success() {
        // Given
        CustomerResponse deactivatedResponse = CustomerResponse.builder()
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
                .message("Customer deactivated successfully")
                .build();

        when(customerService.deactivateCustomer("507f1f77bcf86cd799439011"))
                .thenReturn(Mono.just(deactivatedResponse));

        // When & Then
        webTestClient.patch()
                .uri("/api/v1/customers/507f1f77bcf86cd799439011/deactivate")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("INACTIVE");
    }
}