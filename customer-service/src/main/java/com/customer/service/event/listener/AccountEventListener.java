package com.customer.service.event.listener;

import com.customer.service.event.AccountCreatedEvent;
import com.customer.service.repository.CustomerRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;

@Component
public class AccountEventListener {

    private final CustomerRepository customerRepository;

    public AccountEventListener(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @RabbitListener(queues = "account-created-queue")
    public void handleAccountCreatedEvent(AccountCreatedEvent event) {
        customerRepository.findById(event.getCustomerId())
                .flatMap(customer -> {
                    if (customer.getAccountIds() == null) {
                        customer.setAccountIds(new ArrayList<>());
                    }
                    customer.getAccountIds().add(event.getAccountId());
                    customer.setUpdatedAt(Instant.now());
                    return customerRepository.save(customer);
                })
                .doOnSuccess(c -> System.out.println("✅ Account linked to Customer: " + event.getCustomerId()))
                .doOnError(err -> System.err.println("❌ Error linking account: " + err.getMessage()))
                .subscribe();
    }
}

