package com.transaction.service.config;

import com.transaction.service.messaging.Queues.QueueConstants;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Bean
    public DirectExchange exchange() {
        return new DirectExchange(QueueConstants.EXCHANGE); // "payment-exchange"
    }

    // --- Queues transaction-service LISTENS to ---
    @Bean
    public Queue debitRequestQueue() {
        return new Queue(QueueConstants.DEBIT_REQUEST_QUEUE, true);
    }

    @Bean
    public Queue creditRequestQueue() {
        return new Queue(QueueConstants.CREDIT_REQUEST_QUEUE, true);
    }

    // --- Queues transaction-service PUBLISHES to ---
    @Bean
    public Queue debitResponseQueue() {
        return new Queue(QueueConstants.DEBIT_RESPONSE_QUEUE, true);
    }

    @Bean
    public Queue creditResponseQueue() {
        return new Queue(QueueConstants.CREDIT_RESPONSE_QUEUE, true);
    }

    @Bean
    public Binding debitResponseBinding() {
        return BindingBuilder.bind(debitResponseQueue())
                .to(exchange())
                .with(QueueConstants.DEBIT_RESPONSE);
    }

    @Bean
    public Binding creditResponseBinding() {
        return BindingBuilder.bind(creditResponseQueue())
                .to(exchange())
                .with(QueueConstants.CREDIT_RESPONSE);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public AmqpTemplate amqpTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}