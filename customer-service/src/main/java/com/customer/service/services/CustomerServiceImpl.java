package com.customer.service.services;


import com.customer.service.dto.CustomerNotification;
import com.customer.service.dto.CustomerRequest;
import com.customer.service.dto.CustomerResponse;
import com.customer.service.exception.CustomerCreationException;
import com.customer.service.exception.CustomerDeletionException;
import com.customer.service.exception.CustomerNotFoundException;
import com.customer.service.exception.CustomerUpdateException;
import com.customer.service.model.Customer;
import com.customer.service.model.CustomerEvent;
import com.customer.service.repository.CustomerRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
@Slf4j
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final KafkaTemplate<String, CustomerEvent> kafkaTemplate;
    private final RabbitTemplate rabbitTemplate;

    private final SimpMessagingTemplate messagingTemplate;

    @Value("${rabbitmq.exchange.customer:customer-exchange}")
    private String customerExchange;

    @Value("${rabbitmq.routing-key.customer:customer.routing.key}")
    private String customerRoutingKey;

    @Value("${messaging.enabled:false}")
    private boolean messagingEnabled;

    private final WebClient accountClient;

    public CustomerServiceImpl(CustomerRepository customerRepository, KafkaTemplate<String, CustomerEvent> kafkaTemplate, RabbitTemplate rabbitTemplate,SimpMessagingTemplate messagingTemplate,WebClient.Builder webClientBuilder) {
        this.customerRepository = customerRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.rabbitTemplate = rabbitTemplate;
        this.messagingTemplate=messagingTemplate;
        this.accountClient= webClientBuilder.baseUrl("http://localhost:8085/api/v1/account").build();
    }

    @Override
    public Mono<CustomerResponse> registerCustomer(CustomerRequest customerRequest) {
        // 1. Generate ID and build entity
        String tenantId = UUID.randomUUID().toString();
        System.out.println("TENANT ID" +tenantId);

        Customer customer = Customer.builder()
//                .id(customerId)
                .tenantId(tenantId)
                .firstName(customerRequest.getFirstName())
                .lastName(customerRequest.getLastName())
                .email(customerRequest.getEmail())
                .phoneNumber(customerRequest.getPhoneNumber())
                .dateOfBirth(customerRequest.getDateOfBirth())
                .status(Customer.CustomerStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .accountIds(new ArrayList<>())
                .build();

        // 2. Save to DB and handle reactively
        return customerRepository.save(customer)
                .flatMap(savedCustomer -> {
                    publishEvents(savedCustomer, "REGISTERED");
                    return Mono.just(mapToResponse(savedCustomer, "Customer registered successfully"));
                })
                .onErrorMap(e -> new CustomerCreationException(
                        "Failed to register customer: " + e.getMessage(), e));
    }

    // Helper method to publish events with error handling
    private void publishEvents(Customer customer, String eventType) {
        if (!messagingEnabled) {
            log.debug("Messaging is disabled. Skipping event publication for customer: {}", customer.getId());
            return;
        }


        // 3. Publish events
        CustomerEvent event = new CustomerEvent(
                customer.getId(),
                customer.getFirstName(),
                customer.getEmail(),
                eventType
        );

        // Publish to Kafka with error handling
        try {
            if (kafkaTemplate != null) {
                kafkaTemplate.send("customer-topic", event);
                log.info("Published event to Kafka: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Failed to publish to Kafka: {}", e.getMessage());
        }

        // Publish to RabbitMQ with error handling
        try {
            if (rabbitTemplate != null) {
                rabbitTemplate.convertAndSend(customerExchange, customerRoutingKey, event);
                log.info("Published event to RabbitMQ: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Failed to publish to RabbitMQ: {}", e.getMessage());
        }

        // Send WebSocket notification (always available)
        try {
            CustomerNotification notification = new CustomerNotification(
                    customer.getId(),
                    customer.getFirstName() + " " + customer.getLastName(),
                    "CUSTOMER_" + eventType,
                    Instant.now()
            );

            messagingTemplate.convertAndSend("/topic/customers", notification);
            messagingTemplate.convertAndSend("/topic/customers/" + customer.getTenantId(), notification);
            log.info("Published WebSocket notification: {}", eventType);
        } catch (Exception e) {
            log.error("Failed to publish WebSocket notification: {}", e.getMessage());
        }
    }

    @Override
    public Mono<CustomerResponse> getCustomerById(String id) {
        return customerRepository.findById(id)
                .map(customer -> CustomerResponse.builder()
                        .id(customer.getId())
                        .tenantId(customer.getTenantId())
                        .firstName(customer.getFirstName())
                        .lastName(customer.getLastName())
                        .email(customer.getEmail())
                        .phoneNumber(customer.getPhoneNumber())
                        .dateOfBirth(customer.getDateOfBirth())
                        .status(customer.getStatus())
                        .createdAt(customer.getCreatedAt())
                        .updatedAt(customer.getUpdatedAt())
                        .accountIds(customer.getAccountIds())
                        .build()
                )
                .switchIfEmpty(Mono.error(new CustomerNotFoundException("Customer not found with id: " + id)));
    }


    @Override
    public Mono<CustomerResponse> updateCustomer(String id, CustomerRequest customerRequest) {
        return customerRepository.findById(id)
                .switchIfEmpty(Mono.error(new CustomerNotFoundException("Customer not found with id: " + id)))
                .flatMap(existingCustomer -> {
                    // Update fields
                    existingCustomer.setFirstName(customerRequest.getFirstName());
                    existingCustomer.setLastName(customerRequest.getLastName());
                    existingCustomer.setEmail(customerRequest.getEmail());
                    existingCustomer.setPhoneNumber(customerRequest.getPhoneNumber());
                    existingCustomer.setDateOfBirth(customerRequest.getDateOfBirth());
                    existingCustomer.setUpdatedAt(Instant.now());

                    return customerRepository.save(existingCustomer);
                })
                .flatMap(updatedCustomer -> {
                    publishEvents(updatedCustomer, "UPDATED");
                    return Mono.just(mapToResponse(updatedCustomer, "Customer updated successfully"));
                })
                .onErrorMap(e -> new CustomerUpdateException(
                        "Failed to update customer: " + e.getMessage(), e));
    }

    @Override
    public Mono<Void> deleteCustomer(String id) {
        return customerRepository.findById(id)
                .switchIfEmpty(Mono.error(new CustomerNotFoundException("Customer not found with id: " + id)))
                .flatMap(customer -> {
                    publishEvents(customer, "DELETED");
                    return customerRepository.delete(customer);
                })
                .onErrorMap(e -> new CustomerDeletionException(
                        "Failed to delete customer: " + e.getMessage(), e));
    }

    @Override
    public Mono<CustomerResponse> deactivateCustomer(String id) {
        return customerRepository.findById(id)
                .switchIfEmpty(Mono.error(new CustomerNotFoundException("Customer not found with id: " + id)))
                .flatMap(customer -> {
                    customer.setStatus(Customer.CustomerStatus.INACTIVE);
                    customer.setUpdatedAt(Instant.now());
                    return customerRepository.save(customer);
                })
                .flatMap(deactivatedCustomer -> {
                    publishEvents(deactivatedCustomer, "DEACTIVATED");
                    return Mono.just(mapToResponse(deactivatedCustomer, "Customer deactivated successfully"));
                });
    }

    @Override
    public Mono<CustomerResponse> activateCustomer(Long id) {
        return customerRepository.findById(id)
                .switchIfEmpty(Mono.error(new CustomerNotFoundException("Customer not found with id: " + id)))
                .flatMap(customer -> {
                    customer.setStatus(Customer.CustomerStatus.ACTIVE);
                    customer.setUpdatedAt(Instant.now());
                    return customerRepository.save(customer);
                })
                .flatMap(activatedCustomer -> {
                    publishEvents(activatedCustomer, "ACTIVATED");
                    return Mono.just(mapToResponse(activatedCustomer, "Customer activated successfully"));
                });
    }

    @Override
    public Flux<CustomerResponse> getAllCustomers() {
        return customerRepository.findAll()
                .map(customer -> mapToResponse(customer, null));
    }

    @Override
    public Flux<CustomerResponse> getCustomersByTenant(String tenantId) {
        return customerRepository.findByTenantId(tenantId)
                .map(customer -> mapToResponse(customer, null));
    }

    @Override
    public Flux<CustomerResponse> getCustomersByStatus(Customer.CustomerStatus status) {
        return customerRepository.findByStatus(status)
                .map(customer -> mapToResponse(customer, null));
    }

    @Override
    public Mono<CustomerResponse> getCustomerByEmail(String email) {
        return customerRepository.findByEmail(email)
                .map(customer -> mapToResponse(customer, null))
                .switchIfEmpty(Mono.error(new CustomerNotFoundException("Customer not found with email: " + email)));
    }

    @Override
    public Mono<Boolean> existsByEmail(String email) {
        return customerRepository.existsByEmail(email);
    }

    @Override
    public Mono<CustomerResponse> addAccountToCustomer(String customerId, String accountId) {
        // Step 1: Fetch account from Account Service to validate it exists
        Mono<Map> accountMono = accountClient.get()
                .uri("/{id}", accountId)
                .retrieve()
                .bodyToMono(Map.class);

        return accountMono.flatMap(account ->
                customerRepository.findById(customerId)
                        .switchIfEmpty(Mono.error(new CustomerNotFoundException("Customer not found with id: " + customerId)))
                        .flatMap(customer -> {
                            if (customer.getAccountIds() == null) {
                                customer.setAccountIds(new ArrayList<>());
                            }
                            if (!customer.getAccountIds().contains(accountId)) {
                                customer.getAccountIds().add(accountId);
                                customer.setUpdatedAt(Instant.now());
                                return customerRepository.save(customer);
                            }
                            return Mono.just(customer);
                        })
                        .flatMap(updatedCustomer -> {
                            // Optional: publish an event that an account was added
                            publishEvents(updatedCustomer, "ACCOUNT_ADDED");
                            return Mono.just(mapToResponse(updatedCustomer, "Account added successfully"));
                        })
        );
    }

    @Override
    public Mono<CustomerResponse> removeAccountFromCustomer(Long customerId, String accountId)throws CustomerNotFoundException {
        return customerRepository.findById(customerId)
                .switchIfEmpty(Mono.error(new CustomerNotFoundException("Customer not found with id: " + customerId)))
                .flatMap(customer -> {
                    if (customer.getAccountIds() != null) {
                        customer.getAccountIds().remove(accountId);
                        customer.setUpdatedAt(Instant.now());
                        return customerRepository.save(customer);
                    }
                    return Mono.just(customer);
                })
                .flatMap(updatedCustomer -> {
                    publishEvents(updatedCustomer, "ACCOUNT_REMOVED");
                    return Mono.just(mapToResponse(updatedCustomer, "Account removed successfully"));
                });
    }

    @Override
    public Flux<CustomerResponse> searchCustomers(String searchTerm) {
        return customerRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                        searchTerm, searchTerm, searchTerm)
                .map(customer -> mapToResponse(customer, null));
    }

    @Override
    public Mono<Long> countCustomersByTenant(String tenantId) {
        return customerRepository.countByTenantId(tenantId);
    }

    @Override
    public Mono<Long> countActiveCustomers() {
        return customerRepository.countByStatus(Customer.CustomerStatus.ACTIVE);
    }


    // Helper method to map Customer to CustomerResponse
    private CustomerResponse mapToResponse(Customer customer, String message) {
        return CustomerResponse.builder()
                .id(customer.getId())
                .tenantId(customer.getTenantId())
                .firstName(customer.getFirstName())
                .lastName(customer.getLastName())
                .email(customer.getEmail())
                .phoneNumber(customer.getPhoneNumber())
                .dateOfBirth(customer.getDateOfBirth())
                .status(customer.getStatus())
                .createdAt(customer.getCreatedAt())
                .updatedAt(customer.getUpdatedAt())
                .accountIds(customer.getAccountIds())
                .message(message)
                .build();
    }



}
