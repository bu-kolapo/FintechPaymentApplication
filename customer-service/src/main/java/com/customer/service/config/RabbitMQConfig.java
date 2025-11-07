package com.customer.service.config;


import org.springframework.amqp.core.*;

import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.support.converter.MessageConverter;


@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "account-exchange";
    public static final String QUEUE_NAME = "account-created-queue";
    public static final String ROUTING_KEY = "account.created";

    @Bean
    public DirectExchange accountExchange() {
        return new DirectExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue accountQueue() {
        return new Queue(QUEUE_NAME, true);
    }

    @Bean
    public Binding binding(Queue accountQueue, DirectExchange accountExchange) {
        return BindingBuilder.bind(accountQueue)
                .to(accountExchange)
                .with(ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}


