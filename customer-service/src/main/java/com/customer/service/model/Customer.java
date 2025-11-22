package com.customer.service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Document(collection = "customerdb")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Customer {

    @Id
    private String id;
    private String tenantId;

    private String firstName;
    private String lastName;
    private String email;
    private String idempotencyKey;
    private String phoneNumber;
    private LocalDate dateOfBirth;

    private CustomerStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    private List<String> accountIds; // optional reference to accounts


    public enum CustomerStatus {
        ACTIVE,
        FAILED,
        INACTIVE,
        SUSPENDED
    }
}