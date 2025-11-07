package com.account.service.services;

import com.account.service.config.RabbitMQConfig;
import com.account.service.dto.AccountRequest;
import com.account.service.event.AccountCreatedEvent;
import com.account.service.exception.AccountCreationException;
import com.account.service.model.Account;
import com.account.service.dto.AccountResponse;
import com.account.service.repository.AccountRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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

    public AccountServiceImpl(AccountRepository accountRepository, RabbitTemplate rabbitTemplate) {
        this.accountRepository = accountRepository;
        this.rabbitTemplate =rabbitTemplate;
    }

    @Override
    public Mono<AccountResponse> createAccount(AccountRequest accountRequest) {

        // ✅ Build the account entity (no need to generate tenantId here)
        Account account = Account.builder()
                .tenantId(accountRequest.getTenantId())         // comes from request or token
                .customerId(accountRequest.getCustomerId())     // from the Customer
                .accountNumber(generateAccountNumber())         // generate custom number
                .currency(accountRequest.getCurrency())
                .balance(accountRequest.getBalance() != null ? accountRequest.getBalance() : BigDecimal.ZERO)
                .status(Account.AccountStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        // ✅ Save to DB reactively, then publish event
        return accountRepository.save(account)
                .flatMap(savedAccount -> {
                    // ✅ Create and populate the event after save
                    AccountCreatedEvent event = new AccountCreatedEvent();
                    event.setAccountId(savedAccount.getId());
                    event.setCustomerId(savedAccount.getCustomerId());
                    event.setTenantId(savedAccount.getTenantId());

                    // ✅ Publish the event
                    publishEvents(event, "ACCOUNT_CREATED");

                    // ✅ Return the response
                    return Mono.just(mapToResponse(savedAccount));
                })
                .onErrorMap(e -> new AccountCreationException(
                        "Failed to create account: " + e.getMessage(), e
                ));
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
                .balance(account.getBalance()!= null ? account.getBalance() : BigDecimal.ZERO)
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



}


