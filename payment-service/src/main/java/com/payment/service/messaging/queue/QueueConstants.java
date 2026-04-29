package com.payment.service.messaging.queue;

public final class QueueConstants {

    private QueueConstants() {}

    public static final String DEBIT_REQUEST_QUEUE = "debit-request-queue";
    public static final String CREDIT_REQUEST_QUEUE = "credit-request-queue";
    public static final String CREDIT_RESPONSE_QUEUE = "credit-response-queue";
    public static final String DEBIT_RESPONSE_QUEUE = "debit-response-queue";

    public static final String EXCHANGE = "payment-exchange";
    public static final String DEBIT_REQUEST = "debit.request";
    public static final String DEBIT_RESPONSE = "debit.response";
    public static final String CREDIT_REQUEST = "credit.request";
    public static final String CREDIT_RESPONSE = "credit.response";

}


