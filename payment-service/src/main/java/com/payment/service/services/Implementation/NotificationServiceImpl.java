package com.payment.service.services.Implementation;

import com.payment.service.model.Payment;
import com.payment.service.services.NotificationService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
@Service
public class NotificationServiceImpl implements NotificationService {

    @Override
    public Mono<Void> notifyUser(Payment payment) {
        // Send email/SMS/push
        System.out.println("Notifying user of payment: " + payment.getId());
        return Mono.empty();
    }

}
