package com.customer.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CustomerRequest {

    @Id
    private String id;
    private String tenantId;
    private String idempotencyKey;

    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private LocalDate dateOfBirth;

    private CustomerStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    private List<String> accountIds; // optional reference to accounts


    public enum CustomerStatus {
        ACTIVE,
        INACTIVE,
        SUSPENDED
    }
}