package com.account.service.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // ── Account ─────────────────────────────────────────────
    public static final String EXCHANGE_NAME   = "account-exchange";
    public static final String QUEUE_NAME      = "account-created-queue";
    public static final String ROUTING_KEY     = "account.created";

    // ── Transaction initiated (account-service publishes) ───
    public static final String TRANSACTION_EXCHANGE     = "transaction-exchange";
    public static final String TRANSACTION_QUEUE        = "transaction-initiated-queue";
    public static final String TRANSACTION_ROUTING_KEY  = "transaction.initiated";

    // ── Transaction completed (account-service consumes) ────
    public static final String TRANSACTION_COMPLETED_QUEUE       = "transaction-completed-queue";
    public static final String TRANSACTION_COMPLETED_ROUTING_KEY = "transaction.completed";

    // ── Account beans ────────────────────────────────────────
    @Bean
    public DirectExchange accountExchange() {
        return new DirectExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue accountQueue() {
        return new Queue(QUEUE_NAME, true);
    }

    @Bean
    public Binding accountBinding(Queue accountQueue, DirectExchange accountExchange) {
        return BindingBuilder.bind(accountQueue)
                .to(accountExchange)
                .with(ROUTING_KEY);
    }

    // ── Transaction initiated beans ──────────────────────────
    @Bean
    public DirectExchange transactionExchange() {
        return new DirectExchange(TRANSACTION_EXCHANGE);
    }

    @Bean
    public Queue transactionQueue() {
        return new Queue(TRANSACTION_QUEUE, true);
    }

    @Bean
    public Binding transactionBinding(Queue transactionQueue,
                                      DirectExchange transactionExchange) {
        return BindingBuilder.bind(transactionQueue)
                .to(transactionExchange)
                .with(TRANSACTION_ROUTING_KEY);
    }

    // ── Transaction completed beans ──────────────────────────
    @Bean
    public Queue transactionCompletedQueue() {
        return new Queue(TRANSACTION_COMPLETED_QUEUE, true);
    }

    @Bean
    public Binding transactionCompletedBinding(DirectExchange transactionExchange) {
        return BindingBuilder.bind(transactionCompletedQueue())
                .to(transactionExchange)
                .with(TRANSACTION_COMPLETED_ROUTING_KEY);
    }

    // ── Shared infrastructure ────────────────────────────────
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public AmqpTemplate customRabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}