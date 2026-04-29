package com.transaction.service.messaging.Queues;

public class QueueConstants {

    // ✅ Same exchange as payment-service
    public static final String EXCHANGE = "payment-exchange";

    // Queues transaction-service LISTENS to
    public static final String DEBIT_REQUEST_QUEUE = "debit-request-queue";
    public static final String CREDIT_REQUEST_QUEUE = "credit-request-queue";

    // Queues transaction-service PUBLISHES to
    public static final String DEBIT_RESPONSE_QUEUE = "debit-response-queue";
    public static final String CREDIT_RESPONSE_QUEUE = "credit-response-queue";
    public static final String DEBIT_RESPONSE = "debit.response";
    public static final String CREDIT_RESPONSE = "credit.response";
}