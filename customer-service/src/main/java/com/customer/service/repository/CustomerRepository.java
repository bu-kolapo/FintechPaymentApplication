package com.customer.service.repository;


import com.customer.service.dto.CustomerResponse;
import com.customer.service.model.Customer;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Repository
public interface CustomerRepository extends ReactiveMongoRepository<Customer,String> {

  
    Mono<Long> countByTenantId(String tenantId);
    Flux<Customer> findByTenantId(String tenantId);
    
    Mono<Customer> findByTenantIdAndEmail(String tenantId, String email);
    Mono<Long> countByStatus(Customer.CustomerStatus active);
    Flux<Customer> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrEmailContainingIgnoreCase(String searchTerm, String searchTerm1, String searchTerm2);
    
    Mono<Customer> findById(Long customerId);
    Flux<Customer> findAll();

    Mono<Boolean> existsByEmail(String email);

    Mono<Customer> findByEmail(String email);

    Flux<Customer> findByStatus(Customer.CustomerStatus status);
}
