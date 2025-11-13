package com.payment.service.repository;


import com.payment.service.model.Payment;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface PaymentRepository extends ReactiveMongoRepository<Payment,String> {

        Mono<Payment> findByTransactionId(String transactionId);


        Mono<Payment> findByTenantIdAndId(String tenantId, String id);

        Flux<Payment> findByTenantIdAndAccountId(String tenantId, String accountId);

        Flux<Payment> findByTenantIdAndCustomerId(String tenantId, String customerId);

        Flux<Payment> findByAccountId(String accountId);
        Flux<Payment> findByStatus(Payment.PaymentStatus status);
}
