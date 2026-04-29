package com.account.service.services;

import com.account.service.client.CustomerClient;
import com.account.service.config.RabbitMQConfig;
import com.account.service.dto.AccountDTO;
import com.account.service.dto.AccountRequest;
import com.account.service.event.AccountCreatedEvent;
import com.account.service.exception.AccountCreationException;
import com.account.service.model.Account;
import com.account.service.dto.AccountResponse;
import com.account.service.repository.AccountRepository;
import com.commonlib.service.IdempotencyService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.security.auth.login.AccountNotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
@Slf4j
public class AccountServiceImpl implements AccountService{



    private  final AccountRepository accountRepository;
    private final CustomerClient customerClient;
    private final RabbitTemplate rabbitTemplate;
    @Value("${messaging.enabled:false}")
    private boolean messagingEnabled;

    private final IdempotencyService idempotencyService;

    public AccountServiceImpl(AccountRepository accountRepository, RabbitTemplate rabbitTemplate,IdempotencyService idempotencyService,CustomerClient customerClient) {
        this.accountRepository = accountRepository;
        this.rabbitTemplate =rabbitTemplate;
        this.idempotencyService=idempotencyService;
        this.customerClient=customerClient;
    }

    @Override
    @CircuitBreaker(name = "accountServiceCB", fallbackMethod = "accountFallback")
    @RateLimiter(name = "accountServiceRL")
    public Mono<AccountResponse> createAccount(AccountRequest accountRequest,
                                               String idempotencyKey,String token) {

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Mono.error(new RuntimeException("Idempotency-Key header is required"));
        }

        return idempotencyService.exists(idempotencyKey)
                .flatMap(exists -> {
                    if (exists) {
                        return idempotencyService.getResponse(idempotencyKey,
                                AccountResponse.class);
                    }
                    return resolveAndCreateAccount(accountRequest, idempotencyKey,token);
                })
                .onErrorMap(e -> new AccountCreationException(
                        "Failed to create account: " + e.getMessage(), e
                ));
    }

    private Mono<AccountResponse> accountFallback(AccountRequest accountRequest, String idempotencyKey,String token, Throwable ex) {
        return Mono.just(
                AccountResponse.builder()
                        .tenantId(accountRequest.getTenantId())
                        .customerId(accountRequest.getCustomerId())
                        .currency(accountRequest.getCurrency())
                        .message("Account service is currently unavailable. Please try again later.")
                        .status(Account.AccountStatus.INACTIVE)
                        .build()
        );
    }

    private Mono<AccountResponse> resolveAndCreateAccount(AccountRequest accountRequest,
                                                          String idempotencyKey,String token) {

        return customerClient.getCustomerById(accountRequest.getCustomerId(),token) // 👈 FETCH CUSTOMER
                .flatMap(customer -> generateAccountNumber()
                        .flatMap(accountNumber -> {

                            Account account = Account.builder()
                                    .tenantId(accountRequest.getTenantId())
                                    .customerId(customer.getId())
                                    .customerName(customer.getFirstName() + " " + customer.getLastName()) // ✅ FIX HERE
                                    .accountNumber(accountNumber)
                                    .currency(accountRequest.getCurrency())
                                    .balance(accountRequest.getOpeningBalance() != null
                                            ? accountRequest.getOpeningBalance()
                                            : BigDecimal.ZERO)
                                    .status(Account.AccountStatus.ACTIVE)
                                    .createdAt(Instant.now())
                                    .updatedAt(Instant.now())
                                    .build();

                            return accountRepository.save(account)
                                    .flatMap(savedAccount -> {
                                        publishAccountCreatedEvent(savedAccount);

                                        AccountResponse response = mapToResponse(savedAccount);

                                        return idempotencyService
                                                .storeResponse(idempotencyKey, response)
                                                .thenReturn(response);
                                    });
                        })
                );
    }

    private void publishAccountCreatedEvent(Account savedAccount) {
        AccountCreatedEvent event = new AccountCreatedEvent();
        event.setAccountId(savedAccount.getId());
        event.setCustomerId(savedAccount.getCustomerId());
        event.setTenantId(savedAccount.getTenantId());
        publishEvents(event, "ACCOUNT_CREATED");
    }

    @Override
    public Mono<AccountResponse> getAccountById(String id) {
        return accountRepository.findById(id)
                .map(account -> AccountResponse.builder()
                        .id(account.getId())
                        .tenantId(account.getTenantId())
                        .customerId(account.getCustomerId())
                        .accountNumber(account.getAccountNumber())
                        .currency(account.getCurrency())
                        .message("Details for Customer Id, " + account.getId())
                        .openingBalance(account.getBalance() != null ? account.getBalance() : BigDecimal.ZERO)
                        .balance(account.getBalance() != null ? account.getBalance() : BigDecimal.ZERO) // ← add this
                        .status(account.getStatus())   // ← use actual status, not hardcoded ACTIVE
                        .createdAt(account.getCreatedAt())   // ← use actual timestamps
                        .updatedAt(account.getUpdatedAt())   // ← not Instant.now()
                        .build()
                )
                .switchIfEmpty(Mono.error(new AccountNotFoundException("Account not found with id: " + id)));
    }



    public Mono<String> generateAccountNumber() {
        String accountNumber = RandomStringUtils.randomNumeric(10);

        return accountRepository.existsByAccountNumber(accountNumber)
                .flatMap(exists -> {
                    if (exists) {
                        return generateAccountNumber(); // retry
                    }
                    return Mono.just(accountNumber);
                });
    }


    // Helper method to publish events with error handling
    private void publishEvents(AccountCreatedEvent event, String eventType) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_NAME,
                    RabbitMQConfig.ROUTING_KEY,
                    event
            );
            System.out.println("Published event: " + eventType + " for Customer " + event.getCustomerId());
        } catch (Exception e) {
            System.err.println("Error publishing event: " + e.getMessage());
        }
    }

    private AccountResponse mapToResponse(Account account) {

        return AccountResponse.builder()
                .id(account.getId())
                .tenantId(account.getTenantId())
                .accountNumber(account.getAccountNumber())
                .currency(account.getCurrency())
                .balance(account.getBalance())
                .openingBalance(account.getBalance()!= null ? account.getBalance() : BigDecimal.ZERO)
                .message("Account Opening for Customer " + account.getCustomerName())
                .status(account.getStatus())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .customerName(account.getCustomerName())
                .customerId(account.getCustomerId())
                .build();
    }

//    @PostConstruct
//    public void testRabbit() {
//        try {
//            rabbitTemplate.convertAndSend("account-exchange", "account.created", "Hello Test");
//            System.out.println("✅ Test message sent to RabbitMQ");
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

    @Override
    public Flux<AccountResponse> getAccountsByTenant(String tenantId) {
        return accountRepository.findByTenantId(tenantId)
                .map(account -> mapToResponse(account));
    }
    @Override
    public Mono<AccountResponse> getAccountByNumber(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .switchIfEmpty(Mono.error(
                        new RuntimeException("Account not found: " + accountNumber)))
                .map(this::mapToResponse);
    }

    @Override
    public Flux<AccountResponse> getAllAccounts() {
        return accountRepository.findAll()
                .map(account -> mapToResponse(account));
    }
    @Override
    public Mono<AccountDTO> updateBalance(String accountId, BigDecimal newBalance) {
        return accountRepository.findById(accountId)
                .flatMap(account -> {
                    account.setBalance(newBalance);
                    return accountRepository.save(account);
                })
                .map(this::toDTO); // convert entity to DTO
    }

    private AccountDTO toDTO(Account account) {
        return AccountDTO.builder()
                .id(account.getId())
                .tenantId(account.getTenantId())
                .customerId(account.getCustomerId())
                .accountNumber(account.getAccountNumber())
                .currency(account.getCurrency())
                .balance(account.getBalance())
                .status(account.getStatus())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();
    }

}


