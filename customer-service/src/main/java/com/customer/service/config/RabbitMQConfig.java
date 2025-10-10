package com.customer.service.config;


import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;



@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "customer-exchange";
    public static final String QUEUE_NAME = "customer-queue";
    public static final String ROUTING_KEY = "customer.registered";


    @Bean
    public DirectExchange customerExchange() {
        return new DirectExchange("customer-exchange");
    }

    @Bean
    public Queue customerQueue() {
        return new Queue("customer-registered-queue", true);
    }

    @Bean
    public Binding binding(Queue customerQueue, DirectExchange customerExchange) {
        return BindingBuilder.bind(customerQueue)
                .to(customerExchange)
                .with("customer.registered");
    }
}
