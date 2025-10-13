package com.customer.service.services;

import com.customer.service.dto.CustomerRequest;
import com.customer.service.dto.CustomerResponse;
import com.customer.service.exception.CustomerCreationException;
import com.customer.service.exception.CustomerNotFoundException;
import com.customer.service.model.Customer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CustomerService {
    Mono<CustomerResponse> registerCustomer(CustomerRequest customerRequest) throws CustomerCreationException;
    Mono<CustomerResponse> getCustomerById(String id);
    Mono<CustomerResponse> updateCustomer(String id, CustomerRequest customerRequest);
    Mono<Void> deleteCustomer(String id);
    Mono<CustomerResponse> deactivateCustomer(String id);
    Mono<CustomerResponse> activateCustomer(Long id);
    Flux<CustomerResponse> getAllCustomers();
    Flux<CustomerResponse> getCustomersByTenant(String tenantId);
    Flux<CustomerResponse> getCustomersByStatus(Customer.CustomerStatus status);
    Mono<CustomerResponse> getCustomerByEmail(String email);
    Mono<Boolean> existsByEmail(String email);
    Mono<CustomerResponse> addAccountToCustomer(String customerId, String accountId);
    Mono<CustomerResponse> removeAccountFromCustomer(Long customerId, String accountId) throws CustomerNotFoundException;
    Flux<CustomerResponse> searchCustomers(String searchTerm);
    Mono<Long> countCustomersByTenant(String tenantId);
    Mono<Long> countActiveCustomers();




}
