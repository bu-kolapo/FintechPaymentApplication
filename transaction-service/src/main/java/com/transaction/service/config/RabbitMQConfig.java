package com.transaction.service.config;


import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;


@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "transaction-exchange";
    public static final String ROUTING_KEY = "transaction.completed";
    public static final String QUEUE_NAME = "transaction-completed-queue";

    public static final String TRANSACTION_EXCHANGE = "transactions-exchange";
    public static final String PAYMENT_QUEUE_NAME = "payment-service-queue";
    public static final String TRANSACTION_ROUTING_KEY = "transactions.created";

    @Bean
    public DirectExchange transactionExchange() {
        return new DirectExchange(EXCHANGE_NAME);
    }

    @Bean
    @Primary
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

//    @Bean
//    @Primary
//    public DirectExchange transactionNameExchange() {
//        return new DirectExchange(TRANSACTION_EXCHANGE);
//    }
    @Bean
    public TopicExchange transactionsExchange() {
        return new TopicExchange(TRANSACTION_EXCHANGE, true, false);
    }

    @Bean
    public Queue PaymentQueue() {
        return new Queue(PAYMENT_QUEUE_NAME, true);
    }

    @Bean
    public Binding transactionBinding(@Qualifier("transactionQueue") Queue queue,
                                       DirectExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(TRANSACTION_ROUTING_KEY);
    }
}