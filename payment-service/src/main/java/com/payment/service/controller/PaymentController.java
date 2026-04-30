package com.payment.service.controller;

import com.payment.service.dto.PaymentRequest;
import com.payment.service.model.Payment;
import com.payment.service.services.PaymentOrchestrator;
import com.payment.service.services.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Payment", description = "Payment processing endpoints")
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentOrchestrator paymentOrchestrator;

    public PaymentController(PaymentService paymentService,PaymentOrchestrator paymentOrchestrator) {
        this.paymentService = paymentService;
        this.paymentOrchestrator=paymentOrchestrator;
    }

    @PostMapping("/payment/process")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Process a payment",
            description = "Initiates a payment between two accounts. Requires Idempotency-Key header."
    )
    public Mono<ResponseEntity<Payment>> processPayment(
            @RequestBody @Valid PaymentRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String token) {
        return paymentOrchestrator.process(request, idempotencyKey, token)
                .map(payment -> ResponseEntity.status(HttpStatus.CREATED).body(payment));
    }

    @GetMapping("/payments")
    @Operation(summary = "Get all payments")
    public Flux<Payment> getAllPayments() {
        return paymentService.getAllPayments();
    }

    @GetMapping("/payment/{id}")
    @Operation(summary = "Get payment by ID")
    public Mono<ResponseEntity<Payment>> getPaymentById(@PathVariable String id) {
        return paymentService.getPaymentById(id)
                .map(ResponseEntity::ok)
                .onErrorReturn(ResponseEntity.notFound().build());
    }
}