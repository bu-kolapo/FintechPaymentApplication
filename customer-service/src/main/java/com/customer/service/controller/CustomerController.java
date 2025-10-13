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
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;
    private final SimpMessagingTemplate messagingTemplate;

    public CustomerController(CustomerService customerService, SimpMessagingTemplate messagingTemplate) {
        this.customerService = customerService;
        this.messagingTemplate = messagingTemplate;
    }

    @PostMapping("/register")
    public Mono<ResponseEntity<CustomerResponse>> registerCustomer(
            @RequestBody @Valid CustomerRequest request) {

        return customerService.registerCustomer(request)
                .doOnSuccess(response -> {
                    // Send WebSocket notification to all connected clients
                    messagingTemplate.convertAndSend(
                            "/topic/customers",
                            new CustomerNotification(
                                    response.getId(),
                                    response.getFirstName() + " " + response.getLastName(),
                                    "CUSTOMER_REGISTERED",
                                    Instant.now()
                            )
                    );
                })
                .map(ResponseEntity::ok)
                .onErrorResume(CustomerCreationException.class, e ->
                        Mono.just(ResponseEntity
                                .status(HttpStatus.BAD_REQUEST)
                                .body(new CustomerResponse(null, e.getMessage()))
                        )
                );
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<CustomerResponse>> getCustomer(@PathVariable String id) {
        return customerService.getCustomerById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping
    public Flux<CustomerResponse> getAllCustomers(
            @RequestParam(required = false) String tenantId) {
        if (tenantId != null) {
            return customerService.getCustomersByTenant(tenantId);
        }
        return customerService.getAllCustomers();
    }
}