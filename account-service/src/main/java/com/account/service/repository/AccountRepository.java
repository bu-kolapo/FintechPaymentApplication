package com.account.service.repository;


import com.account.service.model.Account;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
@Repository
public interface AccountRepository extends ReactiveMongoRepository<Account,String> {

    Mono<Account> findByTenantIdAndId(String tenantId, String id);

    Flux<Account> findByTenantIdAndCustomerId(String tenantId, String customerId);

    Mono<Account> findByTenantIdAndAccountNumber(String tenantId, String accountNumber);

    Mono<Account> findByAccountNumber(String accountNumber);
    Mono<Account> findByCustomerId(String customerId);

}
