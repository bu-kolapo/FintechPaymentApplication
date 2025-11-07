package com.customer.service.config;

import com.customer.service.event.CustomerEvent;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

@Configuration
public class MessagingConfig {

    @Bean
    @ConditionalOnProperty(name = "messaging.kafka.enabled", havingValue = "true", matchIfMissing = false)
    public KafkaTemplate<String, CustomerEvent> kafkaTemplate(ProducerFactory<String, CustomerEvent> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    @Bean
    @ConditionalOnProperty(name = "messaging.rabbitmq.enabled", havingValue = "true", matchIfMissing = false)
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(new Jackson2JsonMessageConverter());
        return template;
    }
}
