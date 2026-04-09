package com.customer.service.controller;



import com.customer.service.dto.CustomerRequest;
import com.customer.service.dto.CustomerResponse;
import com.customer.service.exception.CustomerCreationException;
import com.customer.service.exception.CustomerNotFoundException;
import com.customer.service.model.Customer;
import com.customer.service.services.CustomerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;



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
            @RequestBody CustomerRequest customerRequest,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {

        return customerService.registerCustomer(customerRequest, idempotencyKey)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response))
                .onErrorResume(IllegalArgumentException.class, e ->
                        Mono.just(ResponseEntity.badRequest().<CustomerResponse>build()))
                .onErrorResume(CustomerCreationException.class, e ->
                        Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .<CustomerResponse>build()));
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


    // ─────────────────────────────────────────────────────────
    // UPDATE & DELETE
    // ─────────────────────────────────────────────────────────

    @PutMapping("/customer/{id}")
    public Mono<ResponseEntity<CustomerResponse>> updateCustomer(
            @PathVariable String id,
            @RequestBody CustomerRequest customerRequest) {

        return customerService.updateCustomer(id, customerRequest)
                .map(ResponseEntity::ok)
                .onErrorResume(CustomerNotFoundException.class, e ->
                        Mono.just(ResponseEntity.notFound().<CustomerResponse>build()));
    }

    @DeleteMapping("/customer/{id}")
    public Mono<ResponseEntity<Void>> deleteCustomer(@PathVariable String id) {
        return customerService.deleteCustomer(id)
                .then(Mono.just(ResponseEntity.noContent().<Void>build()))
                .onErrorResume(CustomerNotFoundException.class, e ->
                        Mono.just(ResponseEntity.notFound().<Void>build()));
    }

    // ─────────────────────────────────────────────────────────
    // STATUS MANAGEMENT
    // ─────────────────────────────────────────────────────────

    @PatchMapping("/customer/{id}/deactivate")
    public Mono<ResponseEntity<CustomerResponse>> deactivateCustomer(@PathVariable String id) {
        return customerService.deactivateCustomer(id)
                .map(ResponseEntity::ok)
                .onErrorResume(CustomerNotFoundException.class, e ->
                        Mono.just(ResponseEntity.notFound().<CustomerResponse>build()));
    }

    @PatchMapping("/customer/{id}/activate")
    public Mono<ResponseEntity<CustomerResponse>> activateCustomer(@PathVariable String id) {
        return customerService.activateCustomer(id)
                .map(ResponseEntity::ok)
                .onErrorResume(CustomerNotFoundException.class, e ->
                        Mono.just(ResponseEntity.notFound().<CustomerResponse>build()));
    }

    @GetMapping("/customers/status/{status}")
    public Flux<CustomerResponse> getCustomersByStatus(
            @PathVariable Customer.CustomerStatus status) {
        return customerService.getCustomersByStatus(status);
    }

    // ─────────────────────────────────────────────────────────
    // LOOKUP
    // ─────────────────────────────────────────────────────────

    @GetMapping("/customer/email/{email}")
    public Mono<ResponseEntity<CustomerResponse>> getCustomerByEmail(
            @PathVariable String email) {
        return customerService.getCustomerByEmail(email)
                .map(ResponseEntity::ok)
                .onErrorResume(CustomerNotFoundException.class, e ->
                        Mono.just(ResponseEntity.notFound().<CustomerResponse>build()));
    }

    @GetMapping("/customer/exists/{email}")
    public Mono<ResponseEntity<Boolean>> existsByEmail(@PathVariable String email) {
        return customerService.existsByEmail(email)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/customers/search")
    public Flux<CustomerResponse> searchCustomers(
            @RequestParam String searchTerm) {
        return customerService.searchCustomers(searchTerm);
    }

    // ─────────────────────────────────────────────────────────
    // ACCOUNT MANAGEMENT
    // ─────────────────────────────────────────────────────────

    @DeleteMapping("/customer/{customerId}/account/{accountId}")
    public Mono<ResponseEntity<CustomerResponse>> removeAccountFromCustomer(
            @PathVariable Long customerId,
            @PathVariable String accountId) {

        return customerService.removeAccountFromCustomer(customerId, accountId)
                .map(ResponseEntity::ok)
                .onErrorResume(CustomerNotFoundException.class, e ->
                        Mono.just(ResponseEntity.notFound().<CustomerResponse>build()));
    }

    // ─────────────────────────────────────────────────────────
    // STATS
    // ─────────────────────────────────────────────────────────

    @GetMapping("/customers/count/tenant/{tenantId}")
    public Mono<ResponseEntity<Long>> countCustomersByTenant(
            @PathVariable String tenantId) {
        return customerService.countCustomersByTenant(tenantId)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/customers/count/active")
    public Mono<ResponseEntity<Long>> countActiveCustomers() {
        return customerService.countActiveCustomers()
                .map(ResponseEntity::ok);
    }
}