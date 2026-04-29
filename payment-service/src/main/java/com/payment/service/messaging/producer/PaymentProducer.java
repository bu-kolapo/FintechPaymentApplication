package com.payment.service.messaging.producer;

import com.payment.service.dto.CreditRequest;
import com.payment.service.dto.DebitRequest;
import com.payment.service.messaging.queue.QueueConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentProducer {

    private final RabbitTemplate rabbitTemplate;

    public void sendDebitRequest(DebitRequest request) {
        rabbitTemplate.convertAndSend(QueueConstants.EXCHANGE, QueueConstants.DEBIT_REQUEST, request);
    }

    public void sendCreditRequest(CreditRequest request) {
        rabbitTemplate.convertAndSend(QueueConstants.EXCHANGE, QueueConstants.CREDIT_REQUEST, request);
    }
}