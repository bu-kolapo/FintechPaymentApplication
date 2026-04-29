package com.payment.service.messaging.listener;

import com.payment.service.dto.CreditRequest;
import com.payment.service.messaging.event.TransactionResponse;
import com.payment.service.messaging.producer.PaymentProducer;
import com.payment.service.messaging.queue.QueueConstants;
import com.payment.service.model.Payment;
import com.payment.service.model.Transaction;
import com.payment.service.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventListener {

    private final PaymentRepository repository;
    private final PaymentProducer paymentProducer;



    // =========================
    // DEBIT RESPONSE LISTENER
    // =========================
    @RabbitListener(queues = QueueConstants.DEBIT_RESPONSE_QUEUE)
    public void handleDebitResponse(TransactionResponse response) {

        log.info("📥 Debit response received: {}", response);

        repository.findByIdempotencyKey(response.getIdempotencyKey())
                .flatMap(payment -> {
                    Transaction.TransactionStatus status = Transaction.TransactionStatus.valueOf(response.getStatus());


                    if (status == Transaction.TransactionStatus.SUCCESS) {

                        payment.setDebitTransactionId(response.getTransactionId());
                        payment.setTransactionId(response.getTransactionId());

                        // ✅ SEND CREDIT AFTER SUCCESS
                        paymentProducer.sendCreditRequest(
                                new CreditRequest(
                                        payment.getDestinationAccountId(), // ✅ correct account
                                        payment.getAccountId(), // ✅ correct account
                                        payment.getAmount(),
                                        payment.getTenantId(),
                                        payment.getIdempotencyKey() + "-credit", // keep unique
                                        response.getToken() // 🔥 critical
                                )
                        );

                        payment.setStatus(Payment.PaymentStatus.PROCESSING);

                    } else {
                        payment.setStatus(Payment.PaymentStatus.FAILED);
                    }

                    return repository.save(payment);
                })
                .subscribe();
    }

    // =========================
    // CREDIT RESPONSE LISTENER
    // =========================
    @RabbitListener(queues = QueueConstants.CREDIT_RESPONSE_QUEUE)
    public void handleCreditResponse(TransactionResponse response) {

        log.info("📥 Credit response received: {}", response);

        // 🔥 IMPORTANT: strip "-credit"
        String baseKey = response.getIdempotencyKey().replace("-credit", "");

        repository.findByIdempotencyKey(baseKey)
                .flatMap(payment -> {
                    Transaction.TransactionStatus status = Transaction.TransactionStatus.valueOf(response.getStatus());

                    if (status == Transaction.TransactionStatus.SUCCESS) {

                        payment.setCreditTransactionId(response.getTransactionId());
                        payment.setStatus(Payment.PaymentStatus.SUCCESS);
                        payment.setCompletedAt(Instant.now());
                        payment.setProcessedAt(Instant.now());

                    } else {
                        payment.setStatus(Payment.PaymentStatus.FAILED);
                    }

                    return repository.save(payment);
                })
                .subscribe();
    }
}