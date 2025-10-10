package com.transaction.service.repository;


import com.transaction.service.model.Transaction;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
@Repository
public interface TransactionRepository extends ReactiveMongoRepository<Transaction,String> {
    Mono<Transaction> findByTenantIdAndId(String tenantId, String id);

    Flux<Transaction> findByTenantIdAndPaymentId(String tenantId, String paymentId);

    Flux<Transaction> findByTenantIdAndAccountId(String tenantId, String accountId);
    Flux<Transaction> findByAccountId(String accountId);
    Flux<Transaction> findByPaymentId(String paymentId);

}
