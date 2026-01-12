package com.customer.service.controller;


import com.customer.service.dto.CustomerNotification;
import com.customer.service.dto.CustomerRequest;
import com.customer.service.dto.CustomerResponse;
import com.customer.service.exception.CustomerCreationException;
import com.customer.service.services.CustomerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


import java.time.Instant;

@RestController
@RequestMapping("/api/v1")
public class CustomerController {

    private final CustomerService customerService;
    private final SimpMessagingTemplate messagingTemplate;

    public CustomerController(CustomerService customerService, SimpMessagingTemplate messagingTemplate) {
        this.customerService = customerService;
        this.messagingTemplate = messagingTemplate;
    }

    @PostMapping("/register/customer")
    public Mono<ResponseEntity<CustomerResponse>> registerCustomer(
            @RequestBody @Valid CustomerRequest request) {

        return customerService.registerCustomer(request)
                // Ensure WebSocket notification happens inside reactive chain
                .flatMap(response -> {
                    // Send notification to clients
                    messagingTemplate.convertAndSend(
                            "/topic/customers",
                            new CustomerNotification(
                                    response.getId(),
                                    response.getFirstName() + " " + response.getLastName(),
                                    "CUSTOMER_REGISTERED",
                                    Instant.now()
                            )
                    );
                    return Mono.just(response); // continue chain
                })
                // Map the successful response to ResponseEntity
                .map(ResponseEntity::ok)
                // Handle service exceptions and return 400 with error message
                .onErrorResume(CustomerCreationException.class, e ->
                        Mono.just(ResponseEntity
                                .status(HttpStatus.BAD_REQUEST)
                                .body(new CustomerResponse(null, e.getMessage()))
                        )
                );
    }



    @GetMapping("/customer/{id}")
    public Mono<ResponseEntity<CustomerResponse>> getCustomer(@PathVariable String id) {
        return customerService.getCustomerById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/customers")
    public Flux<CustomerResponse> getAllCustomers(
            @RequestParam(required = false) String tenantId) {
        if (tenantId != null) {
            return customerService.getCustomersByTenant(tenantId);
        }
        return customerService.getAllCustomers();
    }
}