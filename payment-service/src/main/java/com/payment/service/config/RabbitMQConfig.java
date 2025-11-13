package com.payment.service.config;


import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;



@Configuration
public class RabbitMQConfig {

    public static final String TRANSACTION_EXCHANGE = "transactions-exchange";
    public static final String PAYMENT_QUEUE = "payment-service-queue";
    public static final String TRANSACTION_ROUTING_KEY = "transactions.created";

    @Bean
    public TopicExchange transactionExchange() {
        return new TopicExchange(TRANSACTION_EXCHANGE, true, false);
    }

    @Bean
    public Queue paymentQueue() {
        return new Queue(PAYMENT_QUEUE, true); // durable queue
    }

    @Bean
    public Binding paymentBinding(Queue paymentQueue, TopicExchange transactionExchange) {
        return BindingBuilder
                .bind(paymentQueue)
                .to(transactionExchange)
                .with(TRANSACTION_ROUTING_KEY);
    }
}