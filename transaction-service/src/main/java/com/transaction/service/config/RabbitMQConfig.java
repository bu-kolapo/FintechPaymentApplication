package com.transaction.service.config;


import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;



@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "transaction-exchange";
    public static final String ROUTING_KEY = "transaction.completed";
    public static final String QUEUE_NAME = "transaction-completed-queue";

    @Bean
    public DirectExchange transactionExchange() {
        return new DirectExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue transactionQueue() {
        return new Queue(QUEUE_NAME, true);
    }

    @Bean
    public Binding binding(Queue transactionQueue, DirectExchange transactionExchange) {
        return BindingBuilder.bind(transactionQueue)
                .to(transactionExchange)
                .with(ROUTING_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}