package com.account.service.services;

import com.account.service.dto.AccountRequest;
import com.account.service.exception.AccountCreationException;
import com.account.service.model.Account;
import com.account.service.model.AccountCreatedEvent;
import com.account.service.model.AccountResponse;
import com.account.service.repository.AccountRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@Slf4j
public class AccountServiceImpl implements AccountService{

    private static List<String> available_accountNumbers=new ArrayList<>();

    private  final AccountRepository accountRepository;
    private final SimpMessagingTemplate messagingTemplate;
    @Value("${messaging.enabled:false}")
    private boolean messagingEnabled;

    public AccountServiceImpl(AccountRepository accountRepository, SimpMessagingTemplate messagingTemplate) {
        this.accountRepository = accountRepository;
        this.messagingTemplate = messagingTemplate;
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

        // ✅ Save to DB reactively
        return accountRepository.save(account)
                .flatMap(savedAccount -> {
                    // ✅ Publish the event to the message broker (RabbitMQ / Kafka)
                    AccountCreatedEvent event = new AccountCreatedEvent(
                            savedAccount.getCustomerId(),
                            savedAccount.getId(),
                            savedAccount.getTenantId()
                    );
                    publishEvents(event, "ACCOUNT_CREATED");

                    // ✅ Map to response
                    return Mono.just(mapToResponse(savedAccount, "Account created successfully"));
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
    private void publishEvents(AccountCreatedEvent accountCreatedEvent, String eventType) {
        if (!messagingEnabled) {
            log.debug("Messaging is disabled. Skipping event publication for customer: {}", accountCreatedEvent.getId());
            return;
        }

    }

    private AccountResponse mapToResponse(Account account, String message) {

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


}


