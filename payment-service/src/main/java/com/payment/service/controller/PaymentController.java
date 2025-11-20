package com.payment.service.controller;

import com.payment.service.dto.PaymentRequest;
import com.payment.service.model.Payment;
import com.payment.service.services.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/payment")
public class PaymentController {

    private final PaymentService paymentService;



    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/process")
    public Mono<ResponseEntity<Payment>> makePayment(@RequestBody PaymentRequest request) {
        return paymentService.processPayment(request)
                .map(payment -> ResponseEntity.ok(payment))
                .onErrorResume(ex -> Mono.just(ResponseEntity.badRequest().build()));
    }


    @GetMapping
    public Flux<Payment> getAllPayments() {
        return paymentService.getAllPayments();
    }

    @GetMapping("/{id}")
    public Mono<Payment> getPaymentById(@PathVariable String id) {
        return paymentService.getPaymentById(id);
    }
}
