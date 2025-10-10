package com.customer.service.services;


import com.customer.service.dto.CustomerNotification;
import com.customer.service.dto.CustomerRequest;
import com.customer.service.dto.CustomerResponse;
import com.customer.service.exception.CustomerCreationException;
import com.customer.service.model.Customer;
import com.customer.service.model.CustomerEvent;
import com.customer.service.repository.CustomerRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.ArrayList;

@Service
@Transactional
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final KafkaTemplate<String, CustomerEvent> kafkaTemplate;
    private final RabbitTemplate rabbitTemplate;

    private final SimpMessagingTemplate messagingTemplate;

    @Value("${rabbitmq.exchange.customer:customer-exchange}")
    private String customerExchange;

    @Value("${rabbitmq.routing-key.customer:customer.routing.key}")
    private String customerRoutingKey;

    public CustomerServiceImpl(CustomerRepository customerRepository, KafkaTemplate<String, CustomerEvent> kafkaTemplate, RabbitTemplate rabbitTemplate,SimpMessagingTemplate messagingTemplate) {
        this.customerRepository = customerRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.rabbitTemplate = rabbitTemplate;
        this.messagingTemplate=messagingTemplate;
    }

    @Override
    public Mono<CustomerResponse> registerCustomer(CustomerRequest customerRequest) {
        // 1. Generate ID and build entity
//        String customerId = UUID.randomUUID().toString();

        Customer customer = Customer.builder()
//                .id(customerId)
                .tenantId(customerRequest.getTenantId())
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
                    // 3. Publish events
                    CustomerEvent event = new CustomerEvent(
                            savedCustomer.getId(),
                            savedCustomer.getFirstName(),
                            savedCustomer.getEmail(),
                            "REGISTERED"
                    );

                    // Publish to Kafka and RabbitMQ (fire and forget)
                    kafkaTemplate.send("customer-topic", event);
                    rabbitTemplate.convertAndSend(customerExchange, customerRoutingKey, event);


                    // Send WebSocket notification to tenant-specific channel
                    CustomerNotification notification = new CustomerNotification(
                            savedCustomer.getId(),
                            savedCustomer.getFirstName() + " " + savedCustomer.getLastName(),
                            "CUSTOMER_REGISTERED",
                            Instant.now()
                    );

                    // Broadcast to all clients
                    messagingTemplate.convertAndSend("/topic/customers", notification);

                    // 4. Return response DTO
                    return Mono.just(CustomerResponse.builder()
                            .id(savedCustomer.getId())
                            .tenantId(savedCustomer.getTenantId())
                            .firstName(savedCustomer.getFirstName())
                            .lastName(savedCustomer.getLastName())
                            .email(savedCustomer.getEmail())
                            .phoneNumber(savedCustomer.getPhoneNumber())
                            .dateOfBirth(savedCustomer.getDateOfBirth())
                            .status(savedCustomer.getStatus())
                            .createdAt(savedCustomer.getCreatedAt())
                            .updatedAt(savedCustomer.getUpdatedAt())
                            .accountIds(savedCustomer.getAccountIds())
                            .message("Customer registered successfully")
                            .build());
                })
                .onErrorMap(e -> new CustomerCreationException(
                        "Failed to register customer: " + e.getMessage(), e));
    }
}