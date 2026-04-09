package com.account.service.services;

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

    private static List<String> available_accountNumbers=new ArrayList<>();

    private  final AccountRepository accountRepository;
    private final RabbitTemplate rabbitTemplate;
    @Value("${messaging.enabled:false}")
    private boolean messagingEnabled;

    private final IdempotencyService idempotencyService;

    public AccountServiceImpl(AccountRepository accountRepository, RabbitTemplate rabbitTemplate,IdempotencyService idempotencyService) {
        this.accountRepository = accountRepository;
        this.rabbitTemplate =rabbitTemplate;
        this.idempotencyService=idempotencyService;
    }

    @Override
    @CircuitBreaker(name = "accountServiceCB", fallbackMethod = "accountFallback")
    @RateLimiter(name = "accountServiceRL")
    public Mono<AccountResponse> createAccount(AccountRequest accountRequest,
                                               String idempotencyKey) {

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Mono.error(new RuntimeException("Idempotency-Key header is required"));
        }

        return idempotencyService.exists(idempotencyKey)
                .flatMap(exists -> {
                    if (exists) {
                        return idempotencyService.getResponse(idempotencyKey,
                                AccountResponse.class);
                    }
                    return resolveAndCreateAccount(accountRequest, idempotencyKey);
                })
                .onErrorMap(e -> new AccountCreationException(
                        "Failed to create account: " + e.getMessage(), e
                ));
    }

    private Mono<AccountResponse> accountFallback(AccountRequest accountRequest,
                                                  String idempotencyKey,
                                                  Throwable ex) {
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
                                                          String idempotencyKey) {
        Account account = Account.builder()
                .tenantId(accountRequest.getTenantId())
                .customerId(accountRequest.getCustomerId())
                .accountNumber(generateAccountNumber())
                .currency(accountRequest.getCurrency())
                .balance(accountRequest.getOpeningBalance() != null ? accountRequest.getOpeningBalance(): BigDecimal.ZERO)
                .status(Account.AccountStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        return accountRepository.save(account)
                .flatMap(savedAccount -> {
                    publishAccountCreatedEvent(savedAccount);

                    AccountResponse response = mapToResponse(savedAccount);

                    return idempotencyService.storeResponse(idempotencyKey, response)
                            .thenReturn(response);
                });
    }

    private void publishAccountCreatedEvent(Account savedAccount) {
        AccountCreatedEvent event = new AccountCreatedEvent();
        event.setAccountId(savedAccount.getId());
        event.setCustomerId(savedAccount.getCustomerId());
        event.setTenantId(savedAccount.getTenantId());
        publishEvents(event, "ACCOUNT_CREATED");
    }

    @Override
    public Mono<AccountResponse>  getAccountById(String id) {
        return accountRepository.findById(id)
                .map(accountResponse -> AccountResponse.builder()
                        .tenantId(accountResponse .getTenantId())         // comes from request or token
                        .customerId(accountResponse .getCustomerId())     // from the Customer
                        .accountNumber(generateAccountNumber())         // generate custom number
                        .currency(accountResponse .getCurrency())
                        .openingBalance(accountResponse .getBalance()!= null ? accountResponse .getBalance() : BigDecimal.ZERO)
                        .status(Account.AccountStatus.ACTIVE)
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .build()
                )
                .switchIfEmpty(Mono.error(new AccountNotFoundException("Customer not found with id: " + id)));
    }




    public String generateAccountNumber() {
        String accountNumberGenerated = RandomStringUtils.randomNumeric(10);
        if (available_accountNumbers.contains(accountNumberGenerated)) {
            generateAccountNumber();
        } else {
            available_accountNumbers.add(accountNumberGenerated);
        }

        return accountNumberGenerated;
    }

//    public static String generateAccountNumber() {
//        // Example: generate a 10-digit numeric account number
//        return RandomStringUtils.randomNumeric(10);
//    }
//
//    public static String generateTenantId() {
//        // Example: random alphanumeric tenant ID
//        return RandomStringUtils.randomAlphanumeric(8).toUpperCase();
//    }


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
                .accountNumber(generateAccountNumber())
                .currency(account.getCurrency())
                .openingBalance(account.getBalance()!= null ? account.getBalance() : BigDecimal.ZERO)
                .status(account.getStatus())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .customerId(account.getCustomerId())
                .build();
    }

    @PostConstruct
    public void testRabbit() {
        try {
            rabbitTemplate.convertAndSend("account-exchange", "account.created", "Hello Test");
            System.out.println("✅ Test message sent to RabbitMQ");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Flux<AccountResponse> getAccountsByTenant(String tenantId) {
        return accountRepository.findByTenantId(tenantId)
                .map(account -> mapToResponse(account));
    }

    @Override
    public Flux<AccountResponse> getAllAccounts() {
        return accountRepository.findAll()
                .map(account -> mapToResponse(account));
    }

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


